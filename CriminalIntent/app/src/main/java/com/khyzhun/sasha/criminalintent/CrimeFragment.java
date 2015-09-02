package com.khyzhun.sasha.criminalintent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Sasha on 07.08.15.
 */
public class CrimeFragment extends Fragment {

    public static final String EXTRA_CRIME_ID = "com.khyzhun.sasha.criminalintent.crime_id";
    public static final int REQUEST_DATE = 0;
    public static final int REQUEST_PHOTO = 1;

    private static final String TAG = "CrimeFragment";
    private static final String DIALOG_DATE = "date";


    private Crime crime;
    private EditText titleField;
    private Button dateButton;
    private CheckBox solvedCheckBox;
    private ImageButton photoButton;
    private ImageView mPhotoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        UUID crimeId = (UUID)getArguments().getSerializable(EXTRA_CRIME_ID);
        crime = CrimeLab.getInstance(getActivity()).getCrime(crimeId);
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.getInstance(getActivity()).saveCrimes();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                if (hasParentActivity()) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }

                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime, parent, false);

        enableHomeButton();

        wireTitleField(view);
        wireDateButton(view);
        wireSolvedCheckBox(view);
        wirePhotoButton(view);
        wirePhotoView(view);

        return  view;
    }


    private void wirePhotoButton(View view) {
        photoButton = (ImageButton)view.findViewById(R.id.crime_imageButton);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CrimeCameraActivity.class);
                startActivityForResult(intent, REQUEST_PHOTO);
            }
        });

        if (cameraFeatureIsUnavailable()) {
            photoButton.setEnabled(false);
        }
    }


    private boolean cameraFeatureIsUnavailable() {
        PackageManager packageManager = getActivity().getPackageManager();
        return !packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }


    private void enableHomeButton() {
        if (hasParentActivity()) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private boolean hasParentActivity() {
        return NavUtils.getParentActivityName(getActivity()) != null;
    }


    private void showPhoto() {
        // Назначение озображения, полученного на основе фотографии
        Photo p = crime.getPhoto();
        BitmapDrawable b = null;
        if (p != null) {
            String path = getActivity()
                    .getFileStreamPath(p.getFilename()).getAbsolutePath();
            b = PictureUtils.setScaleDrawable(getActivity(), path);
            // TODO: 02.09.2015 "get", a ne "set"
        }
        mPhotoView.setImageDrawable(b);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_DATE) {
            Date crimeDate = (Date)intent.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            crime.setDiscoveredOn(crimeDate);
            SimpleDateFormat simpleDateFormat = getSimpleDateFormat();
            updateDate(simpleDateFormat);
        } else if (requestCode == REQUEST_PHOTO) {
            // Создание нового объекта Photo и связывание его с Сrime
            String filename = intent
                    .getStringExtra(CrimeCameraFragment.EXTRA_PHOTO_FILENAME);
            if (filename != null) {
                Photo p = new Photo(filename);
                crime.setPhoto(p);
                showPhoto();
            }
        }
    }


    private void updateDate(SimpleDateFormat simpleDateFormat) {
        dateButton.setText(simpleDateFormat.format(crime.getDiscoveredOn()));
    }


    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);

        return fragment;
    }


    private void wireSolvedCheckBox(View view) {
        solvedCheckBox = (CheckBox)view.findViewById(R.id.crime_solved);
        solvedCheckBox.setChecked(crime.isSolved());
        solvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                crime.setSolved(isChecked);
            }
        });
    }


    private void wireDateButton(View view) {
        SimpleDateFormat dateFormatter = getSimpleDateFormat();
        dateButton = (Button)view.findViewById(R.id.crime_date);
        updateDate(dateFormatter);

        dateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                DatePickerFragment datePicker = DatePickerFragment.newInstance(crime.getDiscoveredOn());
                datePicker.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                datePicker.show(fragmentManager, DIALOG_DATE);
            }
        });
    }


    private SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("MM/dd/yyyy");
    }


    private void wireTitleField(View view) {
        titleField = (EditText)view.findViewById(R.id.crime_title);
        titleField.setText(crime.getTitle());
        titleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                crime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }


    private void wirePhotoView (View view) {
        mPhotoView = (ImageView) view.findViewById(R.id.crime_imageView);
    }


    @Override
    public void onStart() {
        super.onStart();
        showPhoto();
    }

    @Override
    public void onStop() {
        super.onStop();
        PictureUtils.cleanImageView(mPhotoView);
    }

}
