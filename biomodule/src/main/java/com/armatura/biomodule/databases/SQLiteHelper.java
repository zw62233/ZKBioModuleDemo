package com.armatura.biomodule.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.UserInfo;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by Magic on 2020/9/8
 */
public class SQLiteHelper extends OrmLiteSqliteOpenHelper {

    private final static String DB_NAME = "bio_module.db";
    private final static int DB_VERSION = 3;

    private volatile static SQLiteHelper sqLiteHelper = null;
    private Dao<UserInfo, Long> userInfoDao;
    private Dao<CardInfo, Long> cardInfoDao;

    public static SQLiteHelper getInstance() {
        if (sqLiteHelper == null) {
            synchronized (SQLiteHelper.class) {
                if (sqLiteHelper == null) {
                    sqLiteHelper = new SQLiteHelper(ExApplication.instance());
                }
            }
        }
        return sqLiteHelper;
    }


    public SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, UserInfo.class);
            TableUtils.createTable(connectionSource, CardInfo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                upgradeToV2();
                upgradeToV3(connectionSource);
                break;
            case 2:
                upgradeToV3(connectionSource);
                break;
        }

    }

    public Dao<UserInfo, Long> getUserInfoDao() {
        if (userInfoDao == null) {
            try {
                userInfoDao = getDao(UserInfo.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return userInfoDao;
    }

    public Dao<CardInfo, Long> getCardInfoDao() {
        if (cardInfoDao == null) {
            try {
                cardInfoDao = getDao(CardInfo.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return cardInfoDao;
    }

    @Override
    public void close() {
        super.close();
        userInfoDao = null;
        cardInfoDao = null;
    }

    private void upgradeToV2() {
        try {
            getDao(UserInfo.class).executeRaw("ALTER TABLE `UserInfo` ADD COLUMN avatarIndex INTEGER DEFAULT 0;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * V3:add new table to save card info
     */
    private void upgradeToV3(ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, CardInfo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
