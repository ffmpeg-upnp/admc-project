package com.app.dlna.dmc.gui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import app.dlna.controller.v4.R;

public class AboutDialog extends Dialog {

	public AboutDialog(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dl_about);

		findViewById(R.id.btn_howToUse).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				HelpDialog about = new HelpDialog(getContext());
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
				lp.copyFrom(about.getWindow().getAttributes());
				Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				lp.width = WindowManager.LayoutParams.FILL_PARENT;
				lp.height = WindowManager.LayoutParams.FILL_PARENT;
				about.show();
				about.getWindow().setAttributes(lp);
				AboutDialog.this.dismiss();
			}
		});
		findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				AboutDialog.this.dismiss();
			}
		});
	}
}
