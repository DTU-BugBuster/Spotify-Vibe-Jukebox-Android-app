package com.vibejukebox.jukebox;

import android.os.Parcel;
import android.os.Parcelable;

public class TestObject implements Parcelable{
	
	private String objectId;
	private String mName;
	
	public TestObject()
	{
		mName = "default";
		objectId = "ID####";
	}

	public String getName()
	{
		return mName;
	}
	
	public void setName(String name)
	{
		mName = name;
	}
	
	public String getId()
	{
		return objectId;
	}
	
	public void setId(String id)
	{
		objectId = id;
	}
	
	public static final Parcelable.Creator<TestObject> CREATOR = new Parcelable.Creator<TestObject>() {

		@Override
		public TestObject createFromParcel(Parcel source) {
			return new TestObject(source);
		}

		@Override
		public TestObject[] newArray(int size) {
			return new TestObject[size];
		}
	};
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeString(objectId);
	}

	private TestObject(Parcel in)
	{
		mName = in.readString();
		objectId = in.readString();
	}

}
