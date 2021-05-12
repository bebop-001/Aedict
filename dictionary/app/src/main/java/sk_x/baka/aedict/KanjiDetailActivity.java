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

import java.util.List;

import sk_x.baka.aedict.dict.KanjidicEntry;
import sk_x.baka.aedict.dict.TatoebaSearchTask;
import sk_x.baka.aedict.kanji.KanjiUtils;
import sk_x.baka.aedict.kanji.Radicals;
import sk_x.baka.aedict.kanji.RomanizationEnum;
import sk_x.baka.aedict.util.Check;
import sk_x.baka.aedict.util.Constants;
import sk_x.baka.aedict.util.SearchClickListener;
import sk_x.baka.aedict.util.SearchUtils;
import sk_x.baka.aedict.util.SpanStringBuilder;
import sk_x.baka.autils.DialogUtils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

/**
 * Shows a detail of a single Kanji character.
 * 
 * @author Martin Vysny
 */
public class KanjiDetailActivity extends AbstractActivity {
	static final String INTENTKEY_KANJIDIC_ENTRY = "entry";

	/**
	 * Launches this activity.
	 * 
	 * @param activity
	 *            caller activity, not null.
	 * @param entry
	 *            show this entry, not null.
	 */
	public static void launch(final Context activity, final KanjidicEntry entry) {
		Check.checkNotNull("activity", activity);
		Check.checkNotNull("entry", entry);
		final Intent intent = new Intent(activity, KanjiDetailActivity.class);
		intent.putExtra(INTENTKEY_KANJIDIC_ENTRY, entry);
		activity.startActivity(intent);
	}

	private KanjidicEntry entry;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.kanji_detail);
		entry = (KanjidicEntry) getIntent().getSerializableExtra(INTENTKEY_KANJIDIC_ENTRY);
		MainActivity.recentlyViewed(entry);
		final TextView kanji = (TextView) findViewById(R.id.tanaka_kanji);
		kanji.setText(entry.kanji);
		new SearchClickListener(this, entry.kanji).registerTo(kanji);
		((TextView) findViewById(R.id.stroke)).setText(Integer.toString(entry.strokes));
		((TextView) findViewById(R.id.grade)).setText(entry.grade == null ? "-" : entry.grade.toString());
		final Integer jlpt = entry.getJlpt();
		((TextView) findViewById(R.id.jlpt)).setText(jlpt == null ? "-" : jlpt.toString());
		((TextView) findViewById(R.id.radicals)).setText(Radicals.getRadicals(entry.getKanji()));
		((TextView) findViewById(R.id.radicalNumber)).setText(String.valueOf(entry.radical));
		final SearchUtils utils = new SearchUtils(this);
		utils.setupCopyButton(R.id.copy, R.id.tanaka_kanji);
		findViewById(R.id.showStrokeOrder).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				StrokeOrderActivity.launch(KanjiDetailActivity.this, entry.kanji);
			}
		});
		findViewById(R.id.showRadical).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				KanjiAnalyzeActivity.launch(KanjiDetailActivity.this, Radicals.getRadicals(entry.getKanji()), false);
			}
		});
		findViewById(R.id.addToNotepad).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				NotepadActivity.addAndLaunch(KanjiDetailActivity.this, entry);
			}
		});
		addTextViews(R.id.tanaka_english, null, entry.getEnglish(), false);
		updateContent();
		// display hint
		if (!AedictApp.isInstrumentation) {
			new DialogUtils(this).showInfoOnce(Constants.INFOONCE_CLICKABLE_NOTE, R.string.note, R.string.clickableNote);
		}
	}

	private void updateContent() {
		// compute ONYOMI, KUNYOMI, NAMAE and ENGLISH
		addTextViews(R.id.onyomi, "Onyomi: ", entry.getOnyomi(), true);
		addTextViews(R.id.kunyomi, "Kunyomi: ", entry.getKunyomi(), true);
		addTextViews(R.id.namae, "Namae: ", entry.getNamae(), true);
	}

	private void addTextViews(final int parent, final String name, final List<String> items, final boolean isJapanese) {
		final TextView p = (TextView) findViewById(parent);
		p.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
		p.setMovementMethod(new LinkMovementMethod());
		final SpanStringBuilder sb = new SpanStringBuilder();
		if (name != null) {
			sb.append(sb.newSize(this, 15), name);
		}
		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);
			final String sitem = KanjidicEntry.removeSplits(item);
			if (sitem.trim().length() == 0) {
				continue;
			}
			String query = KanjiUtils.isKatakana(sitem.charAt(0))
					? RomanizationEnum.NihonShiki.toHiragana(RomanizationEnum.NihonShiki.toRomaji(sitem))
					: sitem;
			if (isJapanese) {
				query += " AND " + entry.kanji;
			}
			final Object span = sb.newClickable(new SearchClickListener(this, query));
			sb.append(span, item);
			if (i < items.size() - 1) {
				sb.append(", ");
			}
		}
		p.setText(sb);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		AbstractActivity.addMenuItems(this, menu);
		return true;
	}

	private TatoebaSearchTask tatoebaSearchTask;

	@Override
	protected void onResume() {
		super.onResume();
		if (tatoebaSearchTask == null && entry.isNotNullOrEmpty()) {
			tatoebaSearchTask = new TatoebaSearchTask(this, (ViewGroup) findViewById(R.id.tanakaExamples), entry.getJapanese());
			tatoebaSearchTask.execute(entry.getJapanese());
		}
		// the tanakaSearchTask must be non-null otherwise this will fail.
	}

	@Override
	protected void onStop() {
		if (tatoebaSearchTask.cancel(true)) {
			tatoebaSearchTask = null;
		}
		super.onStop();
	}

}
