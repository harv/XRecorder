package com.haoutil.xposed.xrecorder.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.haoutil.xposed.xrecorder.R;
import com.haoutil.xposed.xrecorder.util.Constants;

public class TwoEditTextPreference extends DialogPreference {
    private EditText et_incoming;
    private EditText et_outgoing;

    public TwoEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateDialogView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.two_edittext, null);

        et_incoming = (EditText) view.findViewById(R.id.et_incoming);
        et_outgoing = (EditText) view.findViewById(R.id.et_outgoing);

        String[] values = getPersistedString(Constants.DEFAULT_FILE_CALLTYPE).split(":");
        et_incoming.setText(values[0]);
        et_outgoing.setText(values[1]);

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            persistString(et_incoming.getText() + ":" + et_outgoing.getText());
        }
    }
}
