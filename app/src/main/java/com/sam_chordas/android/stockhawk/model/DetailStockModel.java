package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DetailStockModel implements Parcelable {
    private final List<Day> days;

    private DetailStockModel(List<Day> days) {
        this.days = days;
    }

    protected DetailStockModel(Parcel in) {
        days = in.createTypedArrayList(Day.CREATOR);
    }

    public static final Creator<DetailStockModel> CREATOR = new Creator<DetailStockModel>() {
        @Override
        public DetailStockModel createFromParcel(Parcel in) {
            return new DetailStockModel(in);
        }

        @Override
        public DetailStockModel[] newArray(int size) {
            return new DetailStockModel[size];
        }
    };

    public List<Day> getDays() {
        return Collections.unmodifiableList(days);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(days);
    }

    public static final class Builder {

        private List<Day> days;

        public Builder(int count) {
            days = new ArrayList<>(count);
        }

        public void addDay(Day day) {
            this.days.add(day);
        }

        public Builder reverseDays() {
            Collections.reverse(days);
            return this;
        }

        public DetailStockModel build() {
            return new DetailStockModel(days);
        }

    }

    public static final class Day implements Parcelable {

        public final long date;
        public final double open;

        private Day(long date, double open) {
            this.date = date;
            this.open = open;
        }

        protected Day(Parcel in) {
            date = in.readLong();
            open = in.readDouble();
        }

        public static final Creator<Day> CREATOR = new Creator<Day>() {
            @Override
            public Day createFromParcel(Parcel in) {
                return new Day(in);
            }

            @Override
            public Day[] newArray(int size) {
                return new Day[size];
            }
        };

        public static Day buildDay(String formattedDate, String formattedOpen) throws ParseException {

            final long date =
                    DetailedStockTaskIntentService.DATE_FORMAT.parse(formattedDate).getTime();
            final double open = Double.parseDouble(formattedOpen);

            return new Day(date, open);

        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(date);
            parcel.writeDouble(open);
        }

    }

}
