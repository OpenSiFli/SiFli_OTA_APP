package com.sifli.sifliapp.modules.devicescan.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class AdRecordStore implements Parcelable {

    public static final Creator<AdRecordStore> CREATOR = new Creator<AdRecordStore>() {
        public AdRecordStore createFromParcel(final Parcel in) {
            return new AdRecordStore(in);
        }

        public AdRecordStore[] newArray(final int size) {
            return new AdRecordStore[size];
        }
    };
    private static final String RECORDS_ARRAY = "records_array";
    private static final String LOCAL_NAME_COMPLETE = "local_name_complete";
    private static final String LOCAL_NAME_SHORT = "local_name_short";
    private final SparseArray<AdRecord> mAdRecords;
    private final String mLocalNameComplete;
    private final String mLocalNameShort;

    public AdRecordStore(final Parcel in) {
        final Bundle b = in.readBundle(getClass().getClassLoader());
        mAdRecords = b.getSparseParcelableArray(RECORDS_ARRAY);
        mLocalNameComplete = b.getString(LOCAL_NAME_COMPLETE);
        mLocalNameShort = b.getString(LOCAL_NAME_SHORT);
    }

    /**
     * Instantiates a new Bluetooth LE device Ad Record Store.
     *
     * @param adRecords the ad records
     */
    public AdRecordStore(final SparseArray<AdRecord> adRecords) {
        mAdRecords = adRecords;
        mLocalNameComplete = getRecordDataAsString(mAdRecords.get(AdRecord.BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME));
        mLocalNameShort = getRecordDataAsString(mAdRecords.get(AdRecord.BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME));

    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Gets the short local device name.
     *
     * @return the local name complete
     */
    public String getLocalNameComplete() {
        return mLocalNameComplete;
    }

    /**
     * Gets the complete local device name.
     *
     * @return the local name short
     */
    public String getLocalNameShort() {
        return mLocalNameShort;
    }

    /**
     * retrieves an individual record.
     *
     * @param record the record
     * @return the record
     */
    public AdRecord getRecord(final int record) {
        return mAdRecords.get(record);
    }

    /**
     * Gets the record data as string.
     *
     * @param record the record
     * @return the record data as string
     */
    public String getRecordDataAsString(final int record) {
        return getRecordDataAsString(mAdRecords.get(record));
    }

    /**
     * Gets the record as collection.
     *
     * @return the records as collection
     */
    public Collection<AdRecord> getRecordsAsCollection() {
        return Collections.unmodifiableCollection(asList(mAdRecords));
    }

    /**
     * Checks if is record present.
     *
     * @param record the record
     * @return true, if is record present
     */
    public boolean isRecordPresent(final int record) {
        return mAdRecords.indexOfKey(record) >= 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "AdRecordStore [mLocalNameComplete=" + mLocalNameComplete + ", mLocalNameShort=" + mLocalNameShort + "]";
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(final Parcel parcel, final int arg1) {
        final Bundle b = new Bundle();
        b.putString(LOCAL_NAME_COMPLETE, mLocalNameComplete);
        b.putString(LOCAL_NAME_SHORT, mLocalNameShort);
        b.putSparseParcelableArray(RECORDS_ARRAY, mAdRecords);
        parcel.writeBundle(b);
    }

    /**
     * As list.
     *
     * @param <C>         the generic type
     * @param sparseArray the sparse array
     * @return the collection
     */
    public static <C> Collection<C> asList(final SparseArray<C> sparseArray) {
        if (sparseArray == null) return null;
        final Collection<C> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++) {
            arrayList.add(sparseArray.valueAt(i));
        }
        return arrayList;
    }

    public static String getRecordDataAsString(final AdRecord nameRecord) {
        if (nameRecord == null) {
            return "";
        }
        return new String(nameRecord.getData());
    }

    public static SparseArray<AdRecord> parseScanRecordAsSparseArray(final byte[] scanRecord) {
        final SparseArray<AdRecord> records = new SparseArray<>();

        int index = 0;
        while (index < scanRecord.length) {
            final int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            final int type = scanRecord[index] & 0xFF;

            //Done if our record isn't a valid type
            if (type == 0) break;

            final byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.put(type, new AdRecord(length, type, data));

            //Advance
            index += length;
        }

        return records;
    }
}
