/*
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
import java.util.Arrays;
import java.util.List;

import sk_x.baka.aedict.dict.DictEntry;
import sk_x.baka.aedict.dict.DictTypeEnum;
import sk_x.baka.aedict.dict.Dictionary;
import sk_x.baka.aedict.dict.Edict;
import sk_x.baka.aedict.dict.EdictEntry;
import sk_x.baka.aedict.dict.MatcherEnum;
import sk_x.baka.aedict.dict.SearchQuery;
import sk_x.baka.aedict.kanji.Deinflections;
import sk_x.baka.aedict.kanji.Deinflections.Deinflection;
import sk_x.baka.aedict.kanji.VerbDeinflection;
import sk_x.baka.aedict.util.Check;
import sk_x.baka.aedict.util.DictEntryListActions;
import sk_x.baka.aedict.util.InstallFromToc;
import sk_x.baka.autils.DialogUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import static sk_x.baka.aedict.util.UtilsKt.displayBuildInfo;

/**
 * Provides means to search the edict dictionary file.
 * 
 * @author Martin Vysny
 */
@SuppressWarnings("deprecation")
public class MainActivity extends ListActivity {
	private static String baseDir = null;
	public static String getBaseDir() {
		return baseDir;
	}
	private static AssetManager assetManager = null;
	public static AssetManager getAssetManager() { return assetManager; }

	InstallFromToc dictInstaller = null;

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		baseDir = this.getFilesDir().toString();
		assetManager = getAssets();
		findViewById(R.id.advanced).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				final View g = findViewById(R.id.advancedPanel);
				g.setVisibility(g.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
			}
		});
		findViewById(R.id.clearSearchBox).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				((TextView) findViewById(R.id.searchTerm)).setText("");
			}
		});
		final EditText searchTerm = findViewById(R.id.searchTerm);
		dictInstaller = InstallFromToc.Companion
				.getInstance(MainActivity.this.getApplication(), "dictionaries.toc");
		dictInstaller.install(MainActivity.this, new InstallFromToc.BooleanCB() {
			@Override
			public void booleanCb(boolean success) {
				Log.d("booleanCb", "success: " + success);
				Toast.makeText(MainActivity.this,
						getString(R.string.dictInstallStatus, getString(
								(success) ? R.string.installed : R.string.pending)
						),
						Toast.LENGTH_LONG
				).show();
				searchTerm.setEnabled(success);
			}
		});
		if (!AedictApp.isInstrumentation) {
			new DialogUtils(this).showInfoOnce(AedictApp.getVersion(), AedictApp.format(R.string.whatsNew, AedictApp.getVersion()), getString(R.string.whatsNewText));
		}
		((TextView) findViewById(R.id.aedict)).setText("Aedict " + AedictApp.getVersion());
		new DictEntryListActions(this, true, true, true, false) {

			@Override
			protected void onDelete(int itemIndex) {
				final List<DictEntry> rv = AedictApp.getConfig().getRecentlyViewed();
				rv.remove(itemIndex);
				AedictApp.getConfig().setRecentlyViewed(rv);
				invalidateModel();
			}

			@Override
			protected void onDeleteAll() {
				AedictApp.getConfig().setRecentlyViewed(new ArrayList<DictEntry>());
				invalidateModel();
			}

		}.register(getListView());
		final String prefillTerm = getIntent().getStringExtra(INTENTKEY_PREFILL_SEARCH_FIELD);
		if (prefillTerm != null) {
			((TextView) findViewById(R.id.searchTerm)).setText(prefillTerm);
		}
		// setup search controls
		setupSearchControls();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		final MenuItem item = menu.add(R.string.build_info);
		item.setOnMenuItemClickListener(
		new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					displayBuildInfo(MainActivity.this);
					return true;
				}
			}
		);

		AbstractActivity.addMenuItems(this, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		invalidateModel();
		findViewById(R.id.searchTerm).requestFocus();
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	private List<DictEntry> modelCache = null;

	private List<DictEntry> getModel() {
		if (modelCache == null) {
			modelCache = AedictApp.getConfig().getRecentlyViewed();
		}
		return modelCache;
	}

	private void invalidateModel() {
		modelCache = null;
		setModel();
		findViewById(R.id.intro).setVisibility(getModel().isEmpty() ? View.VISIBLE : View.GONE);
		findViewById(R.id.recentlyViewed).setVisibility(getModel().isEmpty() ? View.GONE : View.VISIBLE);
	}
	
	/**
	 * Adds given entry to the "recently viewed" list.
	 * @param entry the entry, not null.
	 */
	public static void recentlyViewed(final DictEntry entry) {
		Check.checkNotNull("entry", entry);
		final List<DictEntry> entries = AedictApp.getConfig().getRecentlyViewed();
		while (entries.size() > 15) {
			entries.remove(entries.size() - 1);
		}
		entries.remove(entry);
		entries.add(0, entry);
		AedictApp.getConfig().setRecentlyViewed(entries);
	}

	/**
	 * Sets the ListView model.
	 */
	private void setModel() {
		setListAdapter(new ArrayAdapter<DictEntry>(this, android.R.layout.simple_list_item_2, getModel()) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TwoLineListItem view = (TwoLineListItem) convertView;
				if (view == null) {
					view = (TwoLineListItem) getLayoutInflater().inflate(android.R.layout.simple_list_item_2, getListView(), false);
				}
				Edict.print(getModel().get(position), view);
				return view;
			}

		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final DictEntry e = getModel().get(position);
		if (!e.isNotNullOrEmpty()) {
			return;
		}
		EdictEntryDetailActivity.launch(this, EdictEntry.fromEntry(e));
	}

	/**
	 * Pre-fill the string under this key in the search box.
	 */
	static final String INTENTKEY_PREFILL_SEARCH_FIELD = "prefillSearchField";

	/**
	 * Launches this activity.
	 * @param activity context.
	 * @param term if not null this string will be filled in the search box.
	 */
	public static void launch(Activity activity, String term) {
		final Intent i = new Intent(activity, MainActivity.class);
		if (term != null) {
			i.putExtra(INTENTKEY_PREFILL_SEARCH_FIELD, term);
		}
		activity.startActivity(i);
	}

	private void setupSearchControls() {
		findViewById(R.id.englishSearch).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				search(false);
			}
		});
		findViewById(R.id.jpSearch).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				search(true);
			}
		});
		final CheckBox deinflect = findViewById(R.id.jpDeinflectVerbs);
		deinflect.setOnCheckedChangeListener(new ComponentUpdater());
		final CheckBox tanaka = findViewById(R.id.searchExamples);
		tanaka.setOnCheckedChangeListener(new ComponentUpdater());
		final CheckBox translate = findViewById(R.id.translate);
		translate.setOnCheckedChangeListener(new ComponentUpdater());
		((EditText)findViewById(R.id.searchTerm)).setOnEditorActionListener(new EditText.OnEditorActionListener() {
			
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				final String text = ((TextView) findViewById(R.id.searchTerm)).getText().toString().trim();
				if (text.length() == 0) {
					return true;
				}
				final boolean isAdvanced = findViewById(R.id.advancedPanel).getVisibility() != View.GONE;
				if (!isAdvanced) {
					// search for jp/en
					final Deinflections d = VerbDeinflection.searchJpDeinflected(text);
					final SearchQuery en = SearchQuery.searchEnEdict(text, true);
					ResultActivity.launch(MainActivity.this, Arrays.asList(en, d.query), d.deinflections);
				} else if (deinflect.isChecked() || translate.isChecked()) {
					search(true);
				}
				else if (tanaka.isChecked()) {
					final SearchQuery jp = SearchQuery.searchTanaka(AedictApp.getConfig().getSamplesDictType(), text, true, AedictApp.getConfig().getSamplesDictLang());
					final SearchQuery en = SearchQuery.searchTanaka(AedictApp.getConfig().getSamplesDictType(), text, false, AedictApp.getConfig().getSamplesDictLang());
					ResultActivity.launch(MainActivity.this, Arrays.asList(en, jp), null);
				}
				return true;
			}
		});
	}

	private void search(final boolean isJapanese) {
		final boolean isAdvanced = findViewById(R.id.advancedPanel).getVisibility() != View.GONE;
		final boolean isTranslate = ((CheckBox) findViewById(R.id.translate)).isChecked();
		final String text = ((TextView) findViewById(R.id.searchTerm)).getText().toString().trim();
		if (text.length() == 0) {
			return;
		}
		if (isAdvanced && isTranslate && isJapanese) {
			KanjiAnalyzeActivity.launch(this, text, true);
			return;
		}
		final boolean isDeinflect = ((CheckBox) findViewById(R.id.jpDeinflectVerbs)).isChecked();
		if (isAdvanced && isDeinflect && isJapanese) {
			final Deinflections q = VerbDeinflection.searchJpDeinflected(text);
			performSearch(q.query, q.deinflections);
			return;
		}
		final boolean isTanaka = ((CheckBox) findViewById(R.id.searchExamples)).isChecked();
		if (isAdvanced && isTanaka) {
			final SearchQuery q = SearchQuery.searchTanaka(AedictApp.getConfig().getSamplesDictType(), text, isJapanese, AedictApp.getConfig().getSamplesDictLang());
			performSearch(q, null);
			return;
		}
		final MatcherEnum matcher = isAdvanced ? MatcherEnum.values()[((Spinner) findViewById(R.id.matcher)).getSelectedItemPosition()] : MatcherEnum.Substring;
		final SearchQuery q = isJapanese
				? SearchQuery.searchJapanese(text, matcher)
				: SearchQuery.searchEnEdict(text, matcher == MatcherEnum.Exact);
		performSearch(q, null);
	}

	private class ComponentUpdater implements OnCheckedChangeListener {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			final Activity activity = MainActivity.this;
			final Spinner matcher = activity.findViewById(R.id.matcher);
			final CheckBox deinflect = activity.findViewById(R.id.jpDeinflectVerbs);
			final CheckBox tanaka = activity.findViewById(R.id.searchExamples);
			final CheckBox translate = activity.findViewById(R.id.translate);
			if (buttonView.getId() == R.id.jpDeinflectVerbs && isChecked) {
				matcher.setSelection(MatcherEnum.Exact.ordinal());
				tanaka.setChecked(false);
				translate.setChecked(false);
			} else if (buttonView.getId() == R.id.searchExamples && isChecked) {
				matcher.setSelection(MatcherEnum.Substring.ordinal());
				deinflect.setChecked(false);
				translate.setChecked(false);
			} else if (buttonView.getId() == R.id.translate && isChecked) {
				deinflect.setChecked(false);
				tanaka.setChecked(false);
			}
			matcher.setEnabled(!deinflect.isChecked() && !tanaka.isChecked() && !translate.isChecked());
			findViewById(R.id.englishSearch).setEnabled(!translate.isChecked() && !deinflect.isChecked());
		}
	}

	private void performSearch(final SearchQuery query, final List<Deinflection> deinflections) {
		ResultActivity.launch(this, query, deinflections);
	}
}
