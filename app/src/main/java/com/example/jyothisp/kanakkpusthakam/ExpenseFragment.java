package com.example.jyothisp.kanakkpusthakam;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.jyothisp.kanakkpusthakam.data.Expense;
import com.example.jyothisp.kanakkpusthakam.data.ExpenseCursorAdapter;
import com.example.jyothisp.kanakkpusthakam.data.TripContract;

/**
 * A simple {@link Fragment} subclass.
 */
public class ExpenseFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    public ExpenseFragment() {
        // Required empty public constructor
    }

    ExpenseCursorAdapter mExpenseAdapter;

    TextInputEditText mEditText;

    Uri mTripUri;

    private View mEmptyView;

    private long[] mIDs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_trip_history, container, false);

        getActivity().getSupportLoaderManager().initLoader(1, null, this);


        mTripUri = getActivity().getIntent().getData();

        FloatingActionButton floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.trip_history_fab);
        floatingActionButton.setImageResource(R.drawable.addcoins);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewExpenseDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String expenseTitle = mEditText.getText().toString().trim();

                        Intent intent = new Intent(getActivity(), ExpenseInputActivity.class);
                        intent.setData(mTripUri);
                        intent.putExtra(TripContract.ExpenseEntry.COLUMN_ITEM, expenseTitle);
                        startActivity(intent);
                    }
                });
            }
        });
        mEmptyView = rootView.findViewById(R.id.fragment_empty_view);
        ListView listView = (ListView) rootView.findViewById(R.id.fragment_list_view);
        listView.setEmptyView(mEmptyView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                Log.v("ExpenseFragment", "onCreate: " + id);
                showDeleteDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteExpense(id);
                    }
                });
            }
        });

        mExpenseAdapter = new ExpenseCursorAdapter(getContext(), null);

        listView.setAdapter(mExpenseAdapter);


        return rootView;
    }


    private void showDeleteDialog(DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.delete_dialog_message);
        builder.setPositiveButton(R.string.delete, onClickListener);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });

        builder.create().show();
    }


    private void showNewExpenseDialog(DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.add_expense_dialog_title);

        final LinearLayout layout = new LinearLayout(getContext());

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(48, 32, 48, 32);

        final TextInputLayout inputLayout = new TextInputLayout(getContext());
        inputLayout.setHint(getResources().getString(R.string.add_expense_dialog_hint));
        mEditText = new TextInputEditText(getContext());
        inputLayout.addView(mEditText);

        layout.addView(inputLayout, layoutParams);

        builder.setView(layout);


        builder.setPositiveButton(R.string.add, onClickListener);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void getIDsFromDB(){

        mIDs = new long[getNumberOfMembers()];

        String selection = TripContract.MembersEntry.TRIP_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mTripUri))};
        Cursor cursor = getActivity().getContentResolver().query(TripContract.MembersEntry.CONTENT_URI, null, selection, selectionArgs, null);
        int idColumnIndex = cursor.getColumnIndexOrThrow(TripContract.MembersEntry._ID);

        int i = 0;
        while (cursor.moveToNext()) {
            mIDs[i] = cursor.getLong(idColumnIndex);
            i++;
        }

        cursor.close();

    }

    private void deleteExpense(long id) {
        String selection = TripContract.ExpenseEntry._ID + "=?";
        String[] selectionArgs = new String[] {String.valueOf(id)};
        Cursor cursor = getActivity().getContentResolver().query(TripContract.ExpenseEntry.CONTENT_URI, null, selection, selectionArgs, null);
        int cashRolledColumnIndex = cursor.getColumnIndex(TripContract.ExpenseEntry.COLUMN_CASH_ROLLED);
        int expenseColumnIndex = cursor.getColumnIndex(TripContract.ExpenseEntry.COLUMN_EXPENSE);
        Log.v("ExpenseFragment", "onCreate: cashRolledColumnIndex: " + cashRolledColumnIndex);
        cursor.moveToFirst();
        String cashString = cursor.getString(cashRolledColumnIndex);
        int[] cash = intArrayFromString(cashString);
        int expense = cursor.getInt(expenseColumnIndex);
        cursor.close();


        cash = invertArray(cash);
        expense = -expense;
        getIDsFromDB();
        for (int i =0 ; i<mIDs.length; i++){
            Uri uri = ContentUris.withAppendedId(TripContract.MembersEntry.CONTENT_URI, mIDs[i]);
            ContentValues values = new ContentValues();
            values.put(TripContract.MembersEntry.COLUMN_CASH_SPENT, cash[i]);
            values.put(TripContract.MembersEntry.COLUMN_EXPENSE, expense);
            getActivity().getContentResolver().update(uri, values, null, null);
        }

        Uri uri = ContentUris.withAppendedId(TripContract.ExpenseEntry.CONTENT_URI, id);
        getActivity().getContentResolver().delete(uri, null, null);
        Toast.makeText(getContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
    }

    private int[] intArrayFromString(String s){
        String splits[] = s.split(",");
        double[] doubles = new double[getNumberOfMembers()];
        int[] ints = new int[getNumberOfMembers()];
        for (int i =0; i<doubles.length; i++){
            ints[i] = (int) Double.parseDouble(splits[i]);
            Log.v("Expense Fragment", " " + ints[i]);
        }
        return ints;
    }

    private int[] invertArray(int[] ints){
        for (int i=0; i<ints.length; i++){
            ints[i] = - ints[i];
            Log.v("Expense Fragment", "invertArray: Inverted Array: " + ints[i]);
        }
        return ints;
    }


    @NonNull
    @Override
    public Loader onCreateLoader(int id, @Nullable Bundle args) {
        String selection = TripContract.ExpenseEntry.TRIP_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(getActivity().getIntent().getData()))};
        return new CursorLoader(getContext(), TripContract.ExpenseEntry.CONTENT_URI, null, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mEmptyView.setVisibility(View.VISIBLE);
        mExpenseAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
        mExpenseAdapter.swapCursor(null);
    }



    private int getNumberOfMembers() {
        String selection = TripContract.MembersEntry.TRIP_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mTripUri))};
        Cursor cursor = getActivity().getContentResolver().query(TripContract.MembersEntry.CONTENT_URI, null, selection, selectionArgs, null);

        int members = cursor.getCount();

        cursor.close();

        return members;
    }
}
