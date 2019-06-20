package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;

public interface UserCustomJMXMBean {

    // attributes
    int getCount();

    void setCount(int count);

    String getLast();

    void setLast(String last);

    String getAverage();

    void setAverage(String average);

    String getTimestamp();

    void setTimestamp(String timestamp);

    // operations
    void reset();

}
