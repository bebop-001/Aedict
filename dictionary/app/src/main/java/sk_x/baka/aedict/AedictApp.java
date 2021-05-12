/**
 *     Aedict - an EDICT browser for Android
 Copyright (C) 2009 Martin Vysny
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sk_x.baka.aedict;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import sk_x.baka.aedict.dict.DictEntry;
import sk_x.baka.aedict.dict.DictTypeEnum;
import sk_x.baka.aedict.dict.Dictionary;
import sk_x.baka.aedict.dict.DictionaryVersions;
import sk_x.baka.aedict.dict.DownloaderService;
import sk_x.baka.aedict.util.BackgroundService;
import sk_x.baka.aedict.util.Iso6393Codes;
import sk_x.baka.autils.DialogUtils;
import sk_x.baka.autils.ListBuilder;
import sk_x.baka.autils.MiscUtils;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The main application class.
 * 
 * @author Martin Vysny
 */
public class AedictApp extends Application implements OnSharedPreferenceChangeListener {

	@Override
	public void onCreate() {
		super.onCreate();
		// tests will create multiple instances of this class
		// if (instance != null) {
		// throw new IllegalStateException("Not a singleton");
		// }
		instance = this;
		DialogUtils.resError = R.string.error;
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		apply(new Config(this), this);
		ds = new DownloaderService();
		bs = new BackgroundService();
	}

	@Override
	public void onTerminate() {
		MiscUtils.closeQuietly(ds);
		MiscUtils.closeQuietly(bs);
		super.onTerminate();
	}

	private volatile DownloaderService ds;
	private volatile BackgroundService bs;

	public static DownloaderService getDownloader() {
		return getApp().ds;
	}

	public static BackgroundService getBackground() {
		return getApp().bs;
	}

	private static AedictApp instance;

	/**
	 * Returns the application instance.
	 * 
	 * @return the instance.
	 */
	private static AedictApp getApp() {
		if (instance == null) {
			throw new IllegalStateException("Not yet initialized");
		}
		return instance;
	}

	/**
	 * Formats given string using {@link Formatter} and returns it.
	 * 
	 * @param resId
	 *            the string id
	 * @param args
	 *            the parameters.
	 * @return a formatted string.
	 */
	public static String format(final int resId, Object... args) {
		final String formatStr = getStr(resId);
		return new Formatter().format(formatStr, args).toString();
	}

	/**
	 * Returns string associated with given resource ID.
	 * 
	 * @param resId
	 *            the string id
	 * @return a formatted string.
	 */
	public static String getStr(final int resId) {
		return getApp().getString(resId);
	}

	/**
	 * Returns the Ambient version.
	 * 
	 * @return the version string or "unknown" if the version is not available.
	 */
	public static String getVersion() {
		return BuildConfig.VERSION_NAME;
	}

	/**
	 * The configuration.
	 * 
	 * @author Martin Vysny
	 */
	public static class Config {
		private final SharedPreferences prefs;

		/**
		 * Constructs new config instance.
		 * 
		 * @param context
		 *            load default shared preferences from this context.
		 */
		Config(final Context context) {
			this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		}

		/**
		 * If true then a notification icon is registered.
		 * 
		 * @return true if the application is always available.
		 */
		synchronized boolean isAlwaysAvailable() {
			return prefs.getBoolean(ConfigActivity.KEY_ALWAYS_AVAILABLE, false);
		}

		/**
		 * The dictionary name to use. If null then the default one should be
		 * used. Applies to EDICT dictionaries only.
		 * 
		 * @return the dictionary name, never null. Returns
		 *         DEFAULT_DICTIONARY_NAME for the default Edict
		 *         file.
		 */
		public synchronized String getDictionaryName() {
			return prefs.getString(ConfigActivity.KEY_DICTIONARY_NAME, null);
		}

		/**
		 * The preference key of the "notepad items" configuration item.
		 */
		static final String KEY_NOTEPAD_ITEMS = "notepadItems2";

		/**:160
		 * Persisted notepad DictEntries.
		 * 
		 * @param category
		 *            the category, 0 is the default one.
		 * @return the notepad items, never null.
		 */
		public synchronized List<DictEntry> getNotepadItems(final int category) {
			try {
				return DictEntry.fromExternalList(prefs.getString(KEY_NOTEPAD_ITEMS + (category == 0 ? "" : "" + category), ""));
			} catch (Exception ex) {
				// this may happen: earlier aedict builds stored the notepad
				// items
				// in a different format
				Log.e(AedictApp.class.getSimpleName(), "Notepad model parsing failed", ex);
				return new ArrayList<>();
			}
		}

		/**
		 * Notepad DictEntries.
		 * 
		 * @param category
		 *            the category, 0 is the default one.
		 * @param notepadItems
		 *            the new notepad items, never null.
		 */
		public synchronized void setNotepadItems(final int category, final List<? extends DictEntry> notepadItems) {
			prefs.edit()
				.putString(KEY_NOTEPAD_ITEMS + (category == 0 ? "" : "" + category), DictEntry.toExternalList(notepadItems))
			.apply();
		}

		static final String KEY_NOTEPAD_CATEGORIES = "notepadCategories";

		/**
		 * Returns a list of notepad categories.
		 * 
		 * @return a list of notepad category names, not null, may be empty.
		 */
		public synchronized List<String> getNotepadCategories() {
			final List<String> result = new ArrayList<>();
			for (final String cat : prefs.getString(KEY_NOTEPAD_CATEGORIES, "").split("@@@@")) {
				if (!MiscUtils.isNullOrEmpty(cat)) {
					result.add(cat);
				}
			}
			return result;
		}

		/**
		 * A list of notepad categories.
		 * 
		 * @param categories
		 *            a list of notepad category names, not null, may be empty.
		 */
		public synchronized void setNotepadCategories(final List<String> categories) {
			final ListBuilder b = new ListBuilder("@@@@");
			for (final String s : categories) {
				b.add(s);
			}
			prefs
				.edit().putString(KEY_NOTEPAD_CATEGORIES, b.toString())
				.apply();
		}

		/**
		 * The preference key of the "notepad items" configuration item.
		 */
		static final String KEY_RECENTLY_VIEWED_ITEMS = "recentlyViewed";

		/**
		 * Recently viewed DictEntries.
		 * 
		 * @return the notepad items, never null.
		 */
		public synchronized List<DictEntry> getRecentlyViewed() {
			return DictEntry.fromExternalList(prefs.getString(KEY_RECENTLY_VIEWED_ITEMS, ""));
		}

		/**
		 * Recently viewed DictEntries.
		 * 
		 * @param notepadItems
		 *            the new notepad items, never null.
		 */
		public synchronized void setRecentlyViewed(final List<? extends DictEntry> notepadItems) {
			prefs.edit()
				.putString(KEY_RECENTLY_VIEWED_ITEMS, DictEntry.toExternalList(notepadItems))
				.apply();
		}

		/**
		 * Sets the dictionary type used to retrieve the example sentences.
		 * @param dict dictionary type, {@link DictTypeEnum#Tatoeba} or {@link DictTypeEnum#Tanaka}.
		 */
		public synchronized void setSamplesDictType(final DictTypeEnum dict) {
			if(dict!=DictTypeEnum.Tatoeba && dict!=DictTypeEnum.Tanaka) {
				throw new RuntimeException("Invalid dict type: "+dict);
			}
			prefs.edit().putString(ConfigActivity.KEY_EXAMPLES_DICT, dict.name()).apply();
		}

		/**
		 * Gets the dictionary type used to retrieve the example sentences.
		 * @return dictionary type, {@link DictTypeEnum#Tatoeba} or {@link DictTypeEnum#Tanaka}.
		 */
		public synchronized DictTypeEnum getSamplesDictType() {
			return DictTypeEnum.valueOf(prefs.getString(ConfigActivity.KEY_EXAMPLES_DICT, "Tanaka"));
		}

		/**
		 * Sets the dictionary language used to retrieve the example sentences.
		 * @param langCode ISO 639-3 language code.
		 */
		public synchronized void setSamplesDictLang(final String langCode) {
			prefs.edit().putString(ConfigActivity.KEY_EXAMPLES_DICT_LANG, langCode).apply();
		}

		/**
		 * Gets the dictionary language used to retrieve the example sentences.
		 * @return ISO 639-3 language code.
		 */
		public synchronized String getSamplesDictLang() {
			return prefs.getString(ConfigActivity.KEY_EXAMPLES_DICT_LANG, Iso6393Codes.LANG_CODE_ENGLISH);
		}

		/**
		 * True if the results should be sorted (the default), false otherwise.
		 * See http://code.google.com/p/aedict/issues/detail?id=56 for details.
		 */
		static final String KEY_SORT = "sort";
		/**
		 * True if the results should be sorted (the default), false otherwise.
		 * See http://code.google.com/p/aedict/issues/detail?id=56 for details.
		 * 
		 * @param sorted
		 *            sort
		 */
		public synchronized void setSorted(final boolean sorted) {
			prefs.edit().putBoolean(KEY_SORT, sorted).apply();
		}

		/**
		 * True if the results should be sorted (the default), false otherwise.
		 * See http://code.google.com/p/aedict/issues/detail?id=56 for details.
		 * 
		 * @return sort
		 */
		public synchronized boolean isSorted() {
			return prefs.getBoolean(KEY_SORT, true);
		}

		/**
		 * Returns the dictionary location on the SD card of currently selected EDICT
		 * dictionary.
		 * 
		 * @return absolute OS-specific location of the dictionary.
		 */
		public String getDictionaryLoc() {
			final Dictionary d = new Dictionary(DictTypeEnum.Edict, getDictionaryName());
			return d.exists() ? d.getDictionaryLocation().getAbsolutePath() : DictTypeEnum.Edict.getDefaultDictionaryPath();
		}
		
		private static final String KEY_CURRENT_DICT_VERSIONS = "currentDictVersions";
		public synchronized void setCurrentDictVersions(DictionaryVersions dv) {
			prefs.edit().putString(KEY_CURRENT_DICT_VERSIONS, dv.toExternal()).apply();
		}
		public synchronized DictionaryVersions getCurrentDictVersions() {
			return DictionaryVersions.fromExternal(prefs.getString(KEY_CURRENT_DICT_VERSIONS, ""));
		}
		private static final String KEY_SERVER_DICT_VERSIONS = "serverDictVersions";
		public synchronized void setServerDictVersions(DictionaryVersions dv) {
			prefs.edit().putString(KEY_SERVER_DICT_VERSIONS, dv.toExternal()).apply();
		}
		public synchronized DictionaryVersions getServerDictVersions() {
			return DictionaryVersions.fromExternal(prefs.getString(KEY_SERVER_DICT_VERSIONS, ""));
		}
	}

	/**
	 * Loads the configuration from a shared preferences.
	 * 
	 * @return the configuration.
	 */
	public static Config getConfig() {
		return new Config(instance);
	}

	private static final int NOTIFICATION_ID = 1;

	/**
	 * Applies values from given configuration file, e.g. removes or adds the
	 * notification icon etc.
	 * 
	 * @param cfg
	 *            non-null configuration
	 */
	@SuppressWarnings("ConstantConditions")
	private static void apply(final Config cfg, final Context c) {
		final NotificationManager nm = (NotificationManager) instance.getSystemService(Context.NOTIFICATION_SERVICE);
		if (!cfg.isAlwaysAvailable()) {
			//noinspection ConstantConditions
			nm.cancel(NOTIFICATION_ID);
		} else {
			final PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
			final Notification.Builder builder = new Notification.Builder(c);
			builder.setContentIntent(contentIntent);
			builder.setContentTitle("Aedict");
			builder.setContentText("A japanese dictionary");
			builder.setSmallIcon(R.drawable.notification);
			final Notification notification = builder.getNotification();
			notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
			nm.notify(NOTIFICATION_ID, notification);
		}
	}

	/**
	 * If true then the instrumentation (testing) is in progress.
	 */
	public static boolean isInstrumentation = false;

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(ConfigActivity.KEY_ALWAYS_AVAILABLE)) {
			apply(getConfig(), this);
		}
	}

	public static final DictionaryVersions MIN_REQUIRED = new DictionaryVersions();
	static {
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Kanjidic, null), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, null), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, "compdic"), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, "enamdict"), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, "wdjteuc"), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, "french-fj"), "20110313");
		MIN_REQUIRED.versions.put(new Dictionary(DictTypeEnum.Edict, "hispadic"), "20110313");
	}
}
