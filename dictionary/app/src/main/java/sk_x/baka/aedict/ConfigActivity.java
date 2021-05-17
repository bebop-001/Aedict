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
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import sk_x.baka.aedict.dict.Dictionary;
import sk_x.baka.aedict.util.Iso6393Codes;
import sk_x.baka.autils.DialogUtils;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * Configures AEdict.
 * 
 * @author Martin Vysny
 */
public class ConfigActivity extends PreferenceActivity {
	/**
	 * Boolean: Always available? (Adds or removes notification icon).
	 */
	public static final String KEY_ALWAYS_AVAILABLE = "alwaysAvailable";
	/**
	 * Which EDICT dictionary to use for search.
	 */
	public static final String KEY_DICTIONARY_NAME = "dictionaryName";
	/**
	 * Performs the SDCard dictionary cleanup.
	 */
	public static final String KEY_SDCARD_CLEANUP = "sdcardCleanup";
	/**
	 * Resets the introduction dialogs - all dialogs will be shown again.
	 */
	public static final String KEY_RESET_INTRODUCTIONS = "resetIntroductions";
	/**
	 * Shows the "About" dialog.
	 */
	public static final String KEY_ABOUT= "about";
	/**
	 * Picks the example dictionary, Tanaka or Tatoeba.
	 */
	public static final String KEY_EXAMPLES_DICT = "examplesDict";
	/**
	 * Picks the Tatoeba language.
	 */
	public static final String KEY_EXAMPLES_DICT_LANG = "examplesDictLang";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.config);
		addPreferencesFromResource(R.xml.preferences);
		final ListPreference dictNames = (ListPreference) findPreference(KEY_EXAMPLES_DICT_LANG);
		final SortedMap<String, String> codes = Iso6393Codes.getSortedLangNames();
		dictNames.setEntries(codes.keySet().toArray(new CharSequence[0]));
		dictNames.setEntryValues(codes.values().toArray(new CharSequence[0]));
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		final String key = preference.getKey();
		if (key.equals(KEY_RESET_INTRODUCTIONS)) {
			final DialogUtils utils = new DialogUtils(ConfigActivity.this);
			utils.clearInfoOccurency();
			utils.showToast(R.string.resetIntroductionsSummary);
			return true;
		}
		if (key.equals(KEY_ABOUT)) {
			AboutActivity.launch(this);
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// components are now initialized in onResume phase, to refresh
		// dictionary list when a new dictionary is downloaded
		final List<String> dictionaries = new ArrayList<String>();
		for(final Dictionary d:Dictionary.listEdictInstalled()){
			dictionaries.add(d.custom==null?Dictionary.DEFAULT_DICTIONARY_NAME:d.custom);
		}
		Collections.sort(dictionaries);
		final ListPreference dictNames = (ListPreference) findPreference(KEY_DICTIONARY_NAME);
		dictNames.setEntries(dictionaries.toArray(new CharSequence[0]));
		dictNames.setEntryValues(dictionaries.toArray(new CharSequence[0]));
	}
}
