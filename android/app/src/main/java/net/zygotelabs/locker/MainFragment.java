package net.zygotelabs.locker;

import net.zygotelabs.locker.dialogs.DisableLockProtectionDialog;
import net.zygotelabs.locker.dialogs.EnableLockProtectionDialog;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment  {
	private CheckBox checkBox;
	private Button button;
    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDPM;
    private RelativeLayout statusLayout;
    private TextView statusTextTitle;
    private TextView statusTextSummary;
    private TextView seekTextValue;
    private SeekBar lockProgress;

    private int mStackLevel = 0;
	private static final int ENABLE_PROTECTION_DIALOG_FRAGMENT = 5;
	private static final int DISABLE_PROTECTION_DIALOG_FRAGMENT = 6;
    private static final int MIN_FAILED_ATTEMPTS = 10;
    private static final int DEFAULT_FAILED_ATTEMPTS = 10;

	/* Our preferences */
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	protected static final int REQUEST_CODE_ENABLE_ADMIN=1;
	
	public MainFragment() {
		
		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mStackLevel = savedInstanceState.getInt("level");
        }
        
		mDeviceAdmin = new ComponentName(getActivity(), DeviceAdmin.class);
		mDPM = (DevicePolicyManager)getActivity().getSystemService(getActivity().DEVICE_POLICY_SERVICE);
		/* Load our preferences */
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = settings.edit();

    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("level", mStackLevel);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		checkBox = (CheckBox) rootView.findViewById(R.id.checkBoxAdmin);
		button = (Button) rootView.findViewById(R.id.buttonApply);
		statusLayout = (RelativeLayout) rootView.findViewById(R.id.top_layout);
		statusTextTitle = (TextView) rootView.findViewById(R.id.textViewTopTitle);
		statusTextSummary = (TextView) rootView.findViewById(R.id.textViewTopTitleSummary);
		seekTextValue = (TextView) rootView.findViewById(R.id.textViewLockerCount);
		lockProgress = (SeekBar) rootView.findViewById(R.id.seekBarLocker);
		
		lockProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 

			   @Override 
			   public void onProgressChanged(SeekBar seekBar, int progress, 
			     boolean fromUser) { 
				   if (progress < MIN_FAILED_ATTEMPTS) {
                       progress = MIN_FAILED_ATTEMPTS;
					   lockProgress.setProgress(progress);
				   }
				   seekTextValue.setText(String.valueOf(progress));
				   editor.putInt("unlockLimit", progress);
				   editor.commit();
				
			   }

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// No-op.
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// No-op.
			}  
			   });
		updateAdminCheck();
		return rootView;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

	    super.onActivityResult(requestCode, resultCode, data);
	    updateAdminCheck();
	    
	    switch(requestCode) {
        case ENABLE_PROTECTION_DIALOG_FRAGMENT:

            if (resultCode == Activity.RESULT_OK) {
            	enableLockProtection();
            } else if (resultCode == Activity.RESULT_CANCELED){
                // After Cancel code.
            }

            break;
        case DISABLE_PROTECTION_DIALOG_FRAGMENT:
        	if (resultCode == Activity.RESULT_OK) {
        		disableLockProtection();
        	}
        	break;
    }
	    
	}
	
	public void onCheckBoxClicked(boolean checked){

	    if (checked) {
	    	
	    	// Launch the activity to have the user enable our admin.
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getActivity().getString(R.string.add_admin_extra_app_text));
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
            
	    }else {
	    	 disableLockProtection();
	    }
	}
	
	private void updateAdminCheck(){   
		adjustAdminUI(isActiveAdmin());

        int savedLimit = settings.getInt("unlockLimit", DEFAULT_FAILED_ATTEMPTS);
        if (savedLimit < MIN_FAILED_ATTEMPTS) {
            savedLimit = MIN_FAILED_ATTEMPTS;
            editor.putInt("unlockLimit", savedLimit);
            editor.commit();
        }
    	lockProgress.setProgress(savedLimit);
    	
	}
	
	private void updateLockStatus(){
		boolean isProtected = settings.getBoolean("lockEnabled", false);
		
    	if (isProtected){
			statusLayout.setBackgroundColor(getResources().getColor(R.color.colorGreen));
    		statusTextTitle.setText(getActivity().getString(R.string.protect));
    		int unlockLimit = settings.getInt("unlockLimit", DEFAULT_FAILED_ATTEMPTS);
            if (unlockLimit < MIN_FAILED_ATTEMPTS) {
                unlockLimit = MIN_FAILED_ATTEMPTS;
            }
    		statusTextSummary.setText(getActivity().getString(R.string.protected_summary_one)
    				+ " " + Integer.toString(unlockLimit) + " "
    				+ getActivity().getString(R.string.protected_summary_two));
    		button.setText(getActivity().getString(R.string.disable));
    		lockProgress.setEnabled(false);
			
    	}else{
    		statusLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));
    		statusTextTitle.setText(getActivity().getString(R.string.not_protected));
    		statusTextSummary.setText(getActivity().getString(R.string.not_protected_summary));
    		button.setText(getActivity().getString(R.string.enable));
    		lockProgress.setEnabled(true);
    	}
	}
	
	private void adjustAdminUI(boolean adminState){
			checkBox.setChecked(adminState);
	    	button.setEnabled(adminState);
	    	if (!adminState){
	    		editor.putBoolean("lockEnabled", false);
	    		editor.commit();
	    	}
	    	updateLockStatus();
	}
	
	 private boolean isActiveAdmin() {
		 return mDPM.isAdminActive(mDeviceAdmin);
	 }
	
	 public void toggleLockProtection(){
		 if (settings.getBoolean("lockEnabled", false)){
			 showDisableProtectionDialog();

		 }else{
			 showEnableProtectionDialog();
			 
		 }

	 }
	 
	 private void enableLockProtection(){
         if (!isActiveAdmin()) {
             Toast.makeText(getActivity(), "Enable Device Admin first.", Toast.LENGTH_LONG).show();
             updateAdminCheck();
             return;
         }

         try {
             int attempts = lockProgress.getProgress();
             if (attempts < MIN_FAILED_ATTEMPTS) {
                 attempts = MIN_FAILED_ATTEMPTS;
                 lockProgress.setProgress(attempts);
             }

		     mDPM.setMaximumFailedPasswordsForWipe(mDeviceAdmin, attempts);
             int current = mDPM.getMaximumFailedPasswordsForWipe(mDeviceAdmin);

		     editor.putInt("unlockLimit", current);
		     editor.putBoolean("lockEnabled", current > 0);
		     editor.commit();

             Toast.makeText(
                     getActivity(),
                     "Wipe limit set to " + current + " failed attempts.",
                     Toast.LENGTH_LONG
             ).show();
		     updateAdminCheck();

         } catch (SecurityException e) {
             editor.putBoolean("lockEnabled", false);
             editor.commit();
             Toast.makeText(getActivity(), "Security error: " + e.getMessage(), Toast.LENGTH_LONG).show();
             updateAdminCheck();

         } catch (Exception e) {
             editor.putBoolean("lockEnabled", false);
             editor.commit();
             Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
             updateAdminCheck();
         }
	 }
	 
	 private void showEnableProtectionDialog() {

		    mStackLevel++;

		    FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
		    Fragment prev = getActivity().getFragmentManager().findFragmentByTag("EnableLockProtectionDialog");
		    if (prev != null) {
		        ft.remove(prev);
		    }
		    ft.addToBackStack(null);
			ft.commit();
            DialogFragment dialogFrag = EnableLockProtectionDialog.newInstance(100);
            dialogFrag.setTargetFragment(this, ENABLE_PROTECTION_DIALOG_FRAGMENT);
            dialogFrag.show(getFragmentManager().beginTransaction(), "EnableLockProtectionDialog");
		    }
	 
	 private void disableLockProtection(){
         try {
             if (mDPM.isAdminActive(mDeviceAdmin)) {
                 mDPM.setMaximumFailedPasswordsForWipe(mDeviceAdmin, 0);
                 mDPM.removeActiveAdmin(mDeviceAdmin);
             }
         } catch (Exception e) {
             Toast.makeText(getActivity(), "Error while disabling: " + e.getMessage(), Toast.LENGTH_LONG).show();
         }

		 editor.putBoolean("lockEnabled", false);
		 editor.commit();
		 updateAdminCheck();
		 checkBox.setChecked(false);
	 }
	 
	 private void showDisableProtectionDialog() {


        mStackLevel++;

        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        Fragment prev = getActivity().getFragmentManager().findFragmentByTag("DisableLockProtectionDialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
		 ft.commit();
         DialogFragment dialogFrag = DisableLockProtectionDialog.newInstance(101);
         dialogFrag.setTargetFragment(this, DISABLE_PROTECTION_DIALOG_FRAGMENT);
         dialogFrag.show(getFragmentManager().beginTransaction(), "DisableLockProtectionDialog");
	 }

}