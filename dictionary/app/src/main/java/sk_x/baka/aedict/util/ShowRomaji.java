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

package sk_x.baka.aedict.util;

import sk_x.baka.aedict.AedictApp;
import sk_x.baka.aedict.R;
import sk_x.baka.aedict.dict.DictEntry;
import sk_x.baka.aedict.kanji.RomanizationEnum;
import sk_x.baka.autils.AndroidUtils;
import sk_x.baka.autils.MiscUtils;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Helps with the romanization process.
 * 
 * @author Martin Vysny
 */
public abstract class ShowRomaji {
	private static final Boolean isShowingRomaji =  false;

	public ShowRomaji() {
		this(null);
	}
	public ShowRomaji(final Boolean isShowingRomaji) {
	}

	public void showRomaji(final Boolean isShowingRomaji) {
	}
	 
	public void register(final Activity a, final Menu menu) {
		final MenuItem item = menu.add(R.string.show_kana);
		item.setOnMenuItemClickListener(AndroidUtils.safe(a, new MenuItem.OnMenuItemClickListener() {

			public boolean onMenuItemClick(MenuItem item) {
				show(false);
				return true;
			}
		}));
		item.setIcon(R.drawable.showkana);
	}

	protected abstract void show(final boolean romaji);

	public void onResume() {
		show(false);
	}

    // no romaji.  sjs
	public static String romanizeIfRequired(final String kana) {
		return kana;
	}
	
	public String getJapanese(final DictEntry e) {
		Check.checkTrue("entry not valid", e.isValid());
		return e.kanji;
	}
}
