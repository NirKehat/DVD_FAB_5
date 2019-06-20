package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;

public class UserCustomJMX implements UserCustomJMXMBean {

    private int count = 0;//The number of times this event has occured
    private String last = "";//The last value for this event
    private String average = "";//The average of this event value since the process was launched
    private String timestamp = "";//The time this event last occured

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String getLast() {
        return this.last;
    }

    @Override
    public void setLast(String last) {
        this.last = last;
    }

    @Override
    public String getAverage() {
        return this.average;
    }

    @Override
    public void setAverage(String average) {
        this.average = average;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void reset() {
        this.count = 0;
        this.last = "";
        this.average = "";
        this.timestamp = "";
    }

}
