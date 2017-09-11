package com.cyberlink.actiondirector.widget;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ResultedDialog extends DialogFragment {
    public interface Owner {
        void onDialogState(int state, int requestCode, int resultCode, Bundle data);
        void onDialogResult(int requestCode, int resultCode, Bundle data);
    }

    public static final String BUNDLE_REQUEST_CODE = "BUNDLE_REQUEST_CODE";
    public static final int RESULT_CANCELED = Activity.RESULT_CANCELED;
    public static final int RESULT_OK = Activity.RESULT_OK;

    private int mRequestCode;
    private int mResultCode = RESULT_CANCELED;
    private Bundle mResultData = null;

    private ResultedDialog.Owner owner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof Owner) {
            owner = (Owner) getActivity();
        }

        mResultData = getArguments();
        parseArgument(mResultData);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int id = getLayoutId();
        if (id < 0) {
            return null;
        } else {
            return inflater.inflate(id, container, false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        owner = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (owner != null) {
            owner.onDialogResult(mRequestCode, mResultCode, mResultData);
        }
    }

    protected void notifyDialogState(int state) {
        if (owner != null) {
            owner.onDialogState(state, mRequestCode, mResultCode, mResultData);
        }
    }

    protected @LayoutRes int getLayoutId() {
        return -1;
    }

    protected View findViewById(int id) {
        View v = getView();
        View r;
        if (v == null) {
            r = null;
        } else {
            r = v.findViewById(id);
        }
        return r;
    }

    protected void parseArgument(Bundle b) {
        if (b == null) return;

        if (b.containsKey(BUNDLE_REQUEST_CODE)) {
            mRequestCode = b.getInt(BUNDLE_REQUEST_CODE);
        }
    }

    public final void setResult(int resultCode) {
        synchronized (this) {
            mResultCode = resultCode;
            mResultData = getArguments();
        }
    }

    public final void setResult(int resultCode, Bundle data) {
        synchronized (this) {
            mResultCode = resultCode;
            mResultData = data;
        }
    }
}
