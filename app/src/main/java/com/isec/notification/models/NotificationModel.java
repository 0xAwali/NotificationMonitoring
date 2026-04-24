package com.isec.notification.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parcelable data model that carries all extracted information from a single notification.
 */
public class NotificationModel implements Parcelable {

    private int id;
    private int priority;
    @NonNull private String packageName = "";
    @NonNull private String title      = "";
    @NonNull private String text       = "";
    @NonNull private String subText    = "";
    @NonNull private String bigText    = "";
    @NonNull private String infoText   = "";
    @NonNull private String channelId  = "";
    private long postTime;
    private boolean ongoing;
    @NonNull private List<String> rawExtras = new ArrayList<>();
    /** MessagingStyle lines — each entry is "Sender: message text" */
    @NonNull private List<String> messages  = new ArrayList<>();
    /** InboxStyle lines — one String per inbox row */
    @NonNull private List<String> textLines = new ArrayList<>();

    public NotificationModel() {}

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public int getPriority()                  { return priority; }
    public void setPriority(int priority)     { this.priority = priority; }

    @NonNull public String getPackageName()               { return packageName; }
    public void setPackageName(@NonNull String v)         { this.packageName = v; }

    @NonNull public String getTitle()                     { return title; }
    public void setTitle(@NonNull String v)               { this.title = v; }

    @NonNull public String getText()                      { return text; }
    public void setText(@NonNull String v)                { this.text = v; }

    @NonNull public String getSubText()                   { return subText; }
    public void setSubText(@NonNull String v)             { this.subText = v; }

    @NonNull public String getBigText()                   { return bigText; }
    public void setBigText(@NonNull String v)             { this.bigText = v; }

    @NonNull public String getInfoText()                  { return infoText; }
    public void setInfoText(@NonNull String v)            { this.infoText = v; }

    @NonNull public String getChannelId()                 { return channelId; }
    public void setChannelId(String v)                    { this.channelId = (v != null ? v : ""); }

    public long getPostTime()                 { return postTime; }
    public void setPostTime(long v)           { this.postTime = v; }

    public boolean isOngoing()                { return ongoing; }
    public void setOngoing(boolean v)         { this.ongoing = v; }

    @NonNull public List<String> getRawExtras()               { return rawExtras; }
    public void setRawExtras(@NonNull List<String> v)         { this.rawExtras = v; }

    @NonNull public List<String> getMessages()                { return messages; }
    public void setMessages(@NonNull List<String> v)          { this.messages = v; }

    @NonNull public List<String> getTextLines()               { return textLines; }
    public void setTextLines(@NonNull List<String> v)         { this.textLines = v; }

    // -------------------------------------------------------------------------
    // Parcelable
    // -------------------------------------------------------------------------

    protected NotificationModel(Parcel in) {
        String s;
        id          = in.readInt();
        priority    = in.readInt();
        s = in.readString();  packageName = s != null ? s : "";
        s = in.readString();  title       = s != null ? s : "";
        s = in.readString();  text        = s != null ? s : "";
        s = in.readString();  subText     = s != null ? s : "";
        s = in.readString();  bigText     = s != null ? s : "";
        s = in.readString();  infoText    = s != null ? s : "";
        s = in.readString();  channelId   = s != null ? s : "";
        postTime    = in.readLong();
        ongoing     = in.readByte() != 0;
        List<String> list;
        list = in.createStringArrayList();  rawExtras = list != null ? list : new ArrayList<>();
        list = in.createStringArrayList();  messages  = list != null ? list : new ArrayList<>();
        list = in.createStringArrayList();  textLines = list != null ? list : new ArrayList<>();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(priority);
        dest.writeString(packageName);
        dest.writeString(title);
        dest.writeString(text);
        dest.writeString(subText);
        dest.writeString(bigText);
        dest.writeString(infoText);
        dest.writeString(channelId);
        dest.writeLong(postTime);
        dest.writeByte((byte) (ongoing ? 1 : 0));
        dest.writeStringList(rawExtras);
        dest.writeStringList(messages);
        dest.writeStringList(textLines);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<NotificationModel> CREATOR = new Creator<>() {
        @Override
        public NotificationModel createFromParcel(Parcel in) { return new NotificationModel(in); }
        @Override
        public NotificationModel[] newArray(int size)        { return new NotificationModel[size]; }
    };
}
