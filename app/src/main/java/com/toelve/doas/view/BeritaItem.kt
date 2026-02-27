package com.toelve.doas.view

import android.os.Parcel
import android.os.Parcelable


data class BeritaItem(
    val id: String,
    val judul: String,
    val isi: String,
    val tanggal: String,
    val foto: String,
    val pdf: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(judul)
        parcel.writeString(isi)
        parcel.writeString(tanggal)
        parcel.writeString(foto)
        parcel.writeString(pdf)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<BeritaItem> {
        override fun createFromParcel(parcel: Parcel) = BeritaItem(parcel)
        override fun newArray(size: Int) = arrayOfNulls<BeritaItem?>(size)
    }
}
