package org.oahega.com.preference;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import org.oahega.com.CameraActivity;
import org.oahega.com.R;
import org.oahega.com.env.Logger;
import org.oahega.com.utils.Settings;

public class PreferenceBottomSheetDialogFragment extends DialogFragment implements OnClickListener {

  private TextView percentsClassifTextView, percentsDetectionTextView, avarageTextView;
  private Logger LOGGER = new Logger("PreferenceBottomSheetDialogFragment");

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.layout_preferences_fragment, container);

//    view.findViewById(R.id.btn_change_camera).setOnClickListener(this);

    view.findViewById(R.id.p_c_minus).setOnClickListener(this);
    percentsClassifTextView = view.findViewById(R.id.c_percents);
    view.findViewById(R.id.p_c_plus).setOnClickListener(this);

    view.findViewById(R.id.p_d_minus).setOnClickListener(this);
    percentsDetectionTextView = view.findViewById(R.id.d_percents);
    view.findViewById(R.id.p_d_plus).setOnClickListener(this);

    view.findViewById(R.id.avarage_minus).setOnClickListener(this);
    avarageTextView = view.findViewById(R.id.avarage_percents);
    view.findViewById(R.id.avarage_plus).setOnClickListener(this);

    return view;


  }


  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
    final View view = getActivity().getLayoutInflater()
        .inflate(R.layout.layout_preferences_fragment, null);

    final Drawable d = new ColorDrawable(Color.BLACK);
    d.setAlpha(130);

    dialog.getWindow().setBackgroundDrawable(d);
    dialog.getWindow().setContentView(view);

    final WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
    params.width = WindowManager.LayoutParams.WRAP_CONTENT;
    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
    params.gravity = Gravity.CENTER;

    dialog.setCanceledOnTouchOutside(true);

    return dialog;
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.settings) {
      PreferenceBottomSheetDialogFragment dialogFragment = new PreferenceBottomSheetDialogFragment();
      FragmentManager fm = getActivity().getSupportFragmentManager();
      dialogFragment.show(fm, "test");
    }
//    if (v.getId() == R.id.btn_change_camera) {
//      LOGGER.d("on click: btn_change_camera");
//      Settings.getInstance().setBeforChange(false);
//      Settings.getInstance().setIsfront(!Settings.getInstance().isIsfront());
//      getActivity().finish();
//      getActivity().startActivity(getActivity().getIntent());
//      return;
//    }

    if (v.getId() == R.id.p_c_plus) {
      LOGGER.d("on click: p_c_plus");
      String threads = percentsClassifTextView.getText().toString().trim();
      int percents = Integer.parseInt(threads);
      if (percents == 100) {
        return;
      }
      percents += 1;
      percentsClassifTextView.setText(String.valueOf(percents));
      Settings.getInstance().setMinClassificationPercentToShow(percents);
    } else if (v.getId() == R.id.p_c_minus) {
      LOGGER.d("on click: p_c_minus");
      String percent = percentsClassifTextView.getText().toString().trim();
      int percents = Integer.parseInt(percent);
      if (percents == 0) {
        return;
      }
      percents -= 1;
      percentsClassifTextView.setText(String.valueOf(percents));
      Settings.getInstance().setMinClassificationPercentToShow(percents);
    }
    if (v.getId() == R.id.p_d_plus) {
      LOGGER.d("on click: p_d_plus");
      String threads = percentsDetectionTextView.getText().toString().trim();
      int percents = Integer.parseInt(threads);
      if (percents == 100) {
        return;
      }
      percents += 1;
      percentsDetectionTextView.setText(String.valueOf(percents));
      Settings.getInstance().setMinDetectionPercentToShow(percents);
    } else if (v.getId() == R.id.p_d_minus) {
      LOGGER.d("on click: p_d_minus");
      String percent = percentsDetectionTextView.getText().toString().trim();
      int percents = Integer.parseInt(percent);
      if (percents == 0) {
        return;
      }
      percents -= 1;
      percentsDetectionTextView.setText(String.valueOf(percents));
      Settings.getInstance().setMinDetectionPercentToShow(percents);
    }
    if (v.getId() == R.id.avarage_plus) {
      LOGGER.d("on click: avarage_plus");
      String threads = avarageTextView.getText().toString().trim();
      int percents = Integer.parseInt(threads);
      if (percents == 100) {
        return;
      }
      percents += 1;
      avarageTextView.setText(String.valueOf(percents));
      Settings.getInstance().setNumOfAvarage(percents);
    } else if (v.getId() == R.id.avarage_minus) {
      LOGGER.d("on click: avarage_minus");
      String percent = avarageTextView.getText().toString().trim();
      int percents = Integer.parseInt(percent);
      if (percents == 0) {
        return;
      }
      percents -= 1;
      avarageTextView.setText(String.valueOf(percents));
      Settings.getInstance().setNumOfAvarage(percents);
    }
  }
}
