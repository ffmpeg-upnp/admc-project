package com.app.dlna.dmc.processor.async;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.view.Window;

import com.app.dlna.dmc.gui.activity.MainActivity;

public abstract class AsyncTaskWithProgressDialog<T, U, V> extends AsyncTask<T, U, V> {

	private ProgressDialog m_dlg;

	public AsyncTaskWithProgressDialog(String message) {
		m_dlg = new ProgressDialog(MainActivity.INSTANCE);
		m_dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		m_dlg.setMessage(message);
		m_dlg.setCancelable(false);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		m_dlg.show();
	}

	@Override
	protected abstract V doInBackground(T... params);

	protected void onPostExecute(V result) {
		m_dlg.dismiss();
	};

}
