package com.node.common.data.model;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;

import static com.node.gost.ulDrawHelper.fromBase64;
import static com.node.utils.AppUtils.decompress;

/*
 * Created by troy379 on 04.04.17.
 */
public class Message implements IMessage,
        MessageContentType.Image, /*this is for default image messages implementation*/
        MessageContentType /*and this one is for custom content type (in this case - voice message)*/ {

    private String id;
    private String text;
    private Date createdAt;
    private User user;
    private Image image;
    private Voice voice;

    static SecureRandom rnd = new SecureRandom();

    public Message(String id, User user, String text) {
        this(id, user, text, new Date());
    }

    public Message(String id, User user, String text, Date createdAt) {
        this.id = id;
        this.text = text;
        this.user = user;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public String getImageUrl() {
        return image == null ? null : image.url;
    }

    public Voice getVoice() {
        return voice;
    }

    public String getStatus() {
        return "Sent";
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    public static class Image {

        private String url;

        public Image(String url) {
            byte[] text_free = fromBase64(url);
            String tmp = "";
            try {

                tmp = decompress(text_free);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.url = tmp;
        }
    }

    public static class Voice {

        private String url;
        private long duration;

        public Voice(String url, long duration) {

/*            byte[] text_free = fromBase64(url);
            String tmp = "";
            try {

                tmp = decompress(text_free);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.url = tmp;*/
            this.url = url;
            this.duration = rnd.nextInt((int)duration);
        }

        public String getUrl() {
            return url;
        }

        public long getDuration() {
            return duration;
        }
    }
}
