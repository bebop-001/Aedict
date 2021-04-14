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

import sk_x.baka.aedict.dict.DictTypeEnum;
import sk_x.baka.aedict.dict.Dictionary;
import sk_x.baka.aedict.dict.DownloaderService;
import sk_x.baka.aedict.dict.DownloaderService.State;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

/**
 * Shows items currently being downloaded by the {@link DownloaderService}.
 * 
 * @author Martin Vysny
 */
public class DownloadActivity extends Activity {
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AssetManager am = getAssets();
		String[] assets = null;
		try {
			assets = am.list("dictionaries/edict-lucene.zip");
			Log.d("assets:", assets.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		setContentView(R.layout.download);
		handler = new Handler();
		final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
		progress.setIndeterminate(false);
		progress.setMax(100);
		update();
		findViewById(R.id.downloadAll).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				for (DictTypeEnum dt : DictTypeEnum.values()) {
					AedictApp.getDownloader().downloadDict(
							new Dictionary(dt, null),
					DownloadActivity.this
					);
				}
				AedictApp.getDownloader().downloadSod(DownloadActivity.this);
			}
		});
		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				AedictApp.getDownloader().cancelCurrentDownload();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		handler.removeCallbacks(updater);
		handler.post(updater);
	}

	@Override
	protected void onStop() {
		handler.removeCallbacks(updater);
		super.onStop();
	}

	private final Runnable updater = new Runnable() {

		public void run() {
			handler.postDelayed(this, 1000L);
			update();
		}

	};

	private void update() {
		final DownloaderService service = AedictApp.getDownloader();
		final State state = service.getState();
		final TextView statusMessage = ((TextView) findViewById(R.id.statusMessage));
		if (state == null) {
			statusMessage.setText(R.string.noActiveDownload);
		} else {
			statusMessage.setText(state.msg);
		}
		final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
		progress.setProgress(state == null || state.isError ? 0 : state.getCompleteness());
		((TextView) findViewById(R.id.queue)).setText(AedictApp.format(R.string.queuedForDownload, service.getDownloadQueue().toString()));
		((TextView) findViewById(R.id.progressText)).setText(state == null || state.isError ? "-" : state.downloaded + "kb / " + state.total + "kb");
	}

	public static void launch(Activity a) {
		a.startActivity(new Intent(a, DownloadActivity.class));
	}
}
