package com.armatura.biomodule.databases;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.manager.FaceManager;
import com.armatura.biomodule.manager.NIRPalmManager;
import com.armatura.biomodule.manager.PalmManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DatabaseHelper {
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();


    private static final String TABLE_USER_INFO = "UserInfo";
    private static final String KEY_USER_ID = "UserId";

    Dao<UserInfo, Long> userInfoDao = null;
    Dao<CardInfo, Long> cardInfoDao = null;

    DatabaseHelper(Context context) {
        userInfoDao = SQLiteHelper.getInstance().getUserInfoDao();
        cardInfoDao = SQLiteHelper.getInstance().getCardInfoDao();
    }

    boolean isUserExist(String userPin) {
        QueryBuilder<UserInfo, Long> queryBuilder = userInfoDao.queryBuilder();
        long count = 0;
        try {
            queryBuilder.setCountOf(true);
            queryBuilder.where().eq(KEY_USER_ID, userPin);
            count = userInfoDao.countOf(queryBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count > 0;
    }


    long insert(UserInfo user) {
        int ret = -1;
        try {
            ret = userInfoDao.create(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    int insertOrUpdate(CardInfo cardInfo) {
        try {
            Dao.CreateOrUpdateStatus status = cardInfoDao.createOrUpdate(cardInfo);
            Log.i(TAG, "insert  " + status.isCreated() + " update " + status.isUpdated());
            return status.getNumLinesChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    boolean insertOrUpdate(UserInfo user) {
        try {
            Dao.CreateOrUpdateStatus orUpdate = userInfoDao.createOrUpdate(user);
            return orUpdate.isUpdated() || orUpdate.isCreated();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    long update(UserInfo user) {
        int ret = -1;
        try {
            ret = userInfoDao.update(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }


    List<UserInfo> queryAllFaceData() {
        List<UserInfo> userInfos;
        try {
            userInfos = userInfoDao.queryBuilder()
                    .orderBy("ID", false)
                    .query();
            long t1 = System.currentTimeMillis();
            for (UserInfo next : userInfos) {
                //add face template into algorithm cache
                byte[] faceFeatureBytes = next.faceFeature;
                if (faceFeatureBytes != null) {
                    FaceManager.getInstance().dbAdd(next.userId, faceFeatureBytes);
                }

                //add palmTemplate into algorithm cache
                byte[] palmFeatureBytes = next.palmFeature1;
                if (palmFeatureBytes != null) {
                    PalmManager.getInstance().dbAdd(next.userId, palmFeatureBytes);
                }
                byte[] palmVeinFeatureBytes = next.palmFeature2;
                if (palmVeinFeatureBytes != null) {
                    NIRPalmManager.getInstance().dbAdd(next.userId, palmVeinFeatureBytes);
                }
            }
            int faceCount = FaceManager.getInstance().dbCount();
            int palmCount = PalmManager.getInstance().dbCount();
            int palmVeinCount = NIRPalmManager.getInstance().dbCount();
            long t2 = System.currentTimeMillis();
            Log.i(TAG, "face count = " + faceCount
                    + ",palm count=" + palmCount
                    + ",palm vein count=" + palmVeinCount
                    + ",cost " + (t2 - t1) + " ms");
        } catch (SQLException e) {
            e.printStackTrace();
            userInfos = new ArrayList<>();
        }
        return userInfos;
    }


    List<UserInfo> fuzzyQueryUserInfoByNameAndUserPin(String name) {
        List<UserInfo> userList;
        try {
            userList = userInfoDao.queryBuilder().where()
                    .like("Name", String.format("%%%s%%", name))
                    .or()
                    .like("UserId", String.format("%%%s%%", name))
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
            userList = new ArrayList<>();
        }
        return userList;
    }

    void deleteAllUserInfo() {
        try {
            GenericRawResults<String[]> rawResults1
                    = userInfoDao.queryRaw(String.format("delete from %s", TABLE_USER_INFO));
            rawResults1.close();
            GenericRawResults<String[]> rawResults2
                    = userInfoDao.queryRaw(String.format("update sqlite_sequence SET seq = 0 where name = '%s'", TABLE_USER_INFO));
            rawResults2.close();
        } catch (SQLException | IOException ignore) {
        }
    }

    boolean deleteUserInfoByUserPin(String userPin) {
        DeleteBuilder<UserInfo, Long> deleteBuilder = userInfoDao.deleteBuilder();

        int affectRow = -1;
        try {
            deleteBuilder.where().eq(KEY_USER_ID, userPin);
            affectRow = deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return affectRow == 1;
    }
}
