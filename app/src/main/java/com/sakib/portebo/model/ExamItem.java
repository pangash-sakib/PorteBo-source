package com.sakib.portebo.model;

public class ExamItem {
    private String id;
    private String subject;
    private String date;

    public ExamItem(String id, String subject, String date) {
        this.id = id;
        this.subject = subject;
        this.date = date;
    }

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getDate() { return date; }
}