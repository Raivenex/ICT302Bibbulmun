package com.example.myapplication;

public class UserData {
    private String name;
    private String phone;
    private int hoursWorked = 0;
    private String section;
    private String skillSet;

    // Default constructor required for calls to DataSnapshot.getValue(UserData.class)
    public UserData() {
    }

    public UserData(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.hoursWorked = 0;
        this.section = "";
        this.skillSet = "";
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getHours(){return hoursWorked;}

    public void setHours(int hours){this.hoursWorked = hours;}
    public String getSection(){ return section;}
    public void setSection(String Section){this.section = Section;}
    public String getSkillSet(){return skillSet;}
    public void setSkillSet(String skillSet){this.skillSet = skillSet;}
}
