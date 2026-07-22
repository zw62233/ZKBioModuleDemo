package com.armatura.biomodule.databases;

import android.content.Context;
import android.util.Log;

import com.armatura.biomodule.activity.base.BaseActivity;
import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.manager.FaceManager;
import com.armatura.biomodule.manager.NIRPalmManager;
import com.armatura.biomodule.manager.PalmManager;
import com.armatura.biomodule.util.FileUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;


import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BioDataUtil {
    public static final List<UserInfo> userFaces_all_List = Collections.synchronizedList(new ArrayList<UserInfo>());
    private static final String TAG = BioDataUtil.class.getSimpleName();
    private static BioDataUtil bioDataUtil = null;
    private DatabaseHelper databaseHelper = null;

    private BioDataUtil(Context c) {
        databaseHelper = new DatabaseHelper(c);
    }

    public static UserInfo findUserInfoFromDatabasesByUserPin(String userPin) {
        UserInfo specialUserInfo = null;

        try {
            specialUserInfo = SQLiteHelper.getInstance().getUserInfoDao().queryBuilder().
                    where().
                    eq("UserId", userPin)
                    .queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return specialUserInfo;
    }

    public static List<UserInfo> queryUserInfoByPage(long pageIndex, long pageSize) {
        List<UserInfo> userInfos = null;
        try {
            userInfos = SQLiteHelper.getInstance().getUserInfoDao().queryBuilder()
                    .limit(pageSize)
                    .offset(pageIndex * pageSize)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userInfos;
    }

    public static long queryUserCount() {
        long count = 0L;
        try {
            count = SQLiteHelper.getInstance().getUserInfoDao().queryBuilder()
                    .countOf();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public static void deleteUserInfoFromList(String userPin) {
        synchronized (userFaces_all_List) {
            Iterator<UserInfo> iterator = userFaces_all_List.iterator();

            while (iterator.hasNext()) {
                UserInfo user = iterator.next();
                if (user.userId.equals(userPin)) {
                    iterator.remove();
                    break;
                }
            }
        }

    }


    public static boolean isBioTemplateExist(String userPin, boolean isFace, boolean isPalm) {
        synchronized (userFaces_all_List) {
            Iterator<UserInfo> iterator = userFaces_all_List.iterator();

            boolean isExist = false;

            while (iterator.hasNext()) {
                UserInfo UserInfo = iterator.next();
                if (UserInfo.userId.equals(userPin)) {
                    if (isFace && UserInfo.faceFeature != null) {
                        isExist = true;
                        break;
                    }
                    if (isPalm && UserInfo.palmFeature1 != null) {
                        isExist = true;
                        break;
                    }
                }
            }
            return isExist;
        }
    }

    public static BioDataUtil instance() {
        if (bioDataUtil == null) {
            synchronized (BioDataUtil.class) {
                if (bioDataUtil == null) {
                    bioDataUtil = new BioDataUtil(BaseActivity.applicationCtxtWeakRef.get());
                }
            }
        }
        return bioDataUtil;
    }


    public static boolean isUserInfoHasCard(String userId) {
        Dao<CardInfo, Long> cardInfoDao = SQLiteHelper.getInstance().getCardInfoDao();
        try {
            return cardInfoDao.queryBuilder().where().eq("userId", userId).countOf() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static UserInfo identifyUserInfoByCard(String card) {
        Dao<UserInfo, Long> userInfoDao = SQLiteHelper.getInstance().getUserInfoDao();
        Dao<CardInfo, Long> cardInfoDao = SQLiteHelper.getInstance().getCardInfoDao();
        QueryBuilder<CardInfo, Long> cardInfoLongQueryBuilder = cardInfoDao.queryBuilder();
        try {
            List<CardInfo> cardInfos = cardInfoLongQueryBuilder
                    .where()
                    .eq("rawCard", card)
                    .query();
            if (cardInfos.isEmpty()) {
                return null;
            }
            CardInfo cardInfo = cardInfos.get(0);
            QueryBuilder<UserInfo, Long> userInfoLongQueryBuilder = userInfoDao.queryBuilder();
            List<UserInfo> userInfos = userInfoLongQueryBuilder
                    .where()
                    .eq("userId", cardInfo.userId)
                    .query();
            if (!userInfos.isEmpty()) {
                return userInfos.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserInfo identifyUserInfoByFaceFeature(byte[] feature) {
        float threshold = Config.instance().faceIdentifyThreshold;

        if (BioDataUtil.userFaces_all_List.isEmpty()) {
            return null;
        }

        byte[] userId = new byte[40];
        float[] score = new float[1];
        int ret = FaceManager.getInstance().dbIdentify(feature, userId, score);
        Log.i(TAG, "identifyUserInfoByFaceFeature: dbIdentify ret = " + ret);
        if (ret != 0) {
            return null;
        }

        String finalUserId = new String(userId, 0, getValidLength(userId));

        Log.i(TAG, "GetRegisteredUserInfoByFaceFeature: userId=" + finalUserId + " score=" + score[0]);
        //compare the score with threshold
        if (score[0] <= Config.instance().faceIdentifyThreshold) {
            Log.w(TAG, "[GetRegisteredUserInfoByFaceFeature]: " + score[0]);
            return null;
        }


        //find user info from memory
        UserInfo userFace_max = null;
        synchronized (BioDataUtil.userFaces_all_List) {
            for (UserInfo userFace : BioDataUtil.userFaces_all_List) {
                if (userFace.userId.equals(finalUserId)) {
                    userFace_max = userFace;
                    break;
                }
            }
        }
        Log.i(TAG, String.format("score=%f threshold= %f", score[0], threshold));
        if (userFace_max != null) {
            //similarity for display
            userFace_max.similarity = score[0];
            return userFace_max;
        }
        return null;
    }


    public List<UserInfo> fuzzyQueryByName(String name) {
        return databaseHelper.fuzzyQueryUserInfoByNameAndUserPin(name);
    }

    public void updateUsers() {
        synchronized (BioDataUtil.userFaces_all_List) {
            userFaces_all_List.clear();
            FaceManager.getInstance().dbClear();
            PalmManager.getInstance().dbClear();
            NIRPalmManager.getInstance().dbClear();
        }
        List<UserInfo> userFaces = databaseHelper.queryAllFaceData();

        synchronized (BioDataUtil.userFaces_all_List) {
            userFaces_all_List.addAll(userFaces);
        }
        userFaces.clear();
    }

    public long insertUserInfo(UserInfo userInfo) {
        long id = databaseHelper.insert(userInfo);
        if (id != -1) {
            if (userInfo.faceFeature != null) {
                FaceManager.getInstance().dbAdd(userInfo.userId, userInfo.faceFeature);
            }
            if (userInfo.palmFeature1 != null) {
                PalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature1);
            }
            if (userInfo.palmFeature2 != null) {
                NIRPalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature2);
            }
        }
        //update memory
        synchronized (BioDataUtil.userFaces_all_List) {
            userFaces_all_List.add(0, userInfo);
        }
        return id;
    }

    public void insertOrUpdateUserInfo(UserInfo userInfo) {
        if (databaseHelper.insertOrUpdate(userInfo)) {
            //update memory
            synchronized (BioDataUtil.userFaces_all_List) {
                userFaces_all_List.remove(userInfo);
                userFaces_all_List.add(userInfo);
            }
            if (userInfo.faceFeature != null) {
                FaceManager.getInstance().dbAdd(userInfo.userId, userInfo.faceFeature);
            }
            if (userInfo.palmFeature1 != null) {
                PalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature1);
            }
            if (userInfo.palmFeature2 != null) {
                NIRPalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature2);
            }
        }
    }

    public long updateUserInfo(UserInfo userInfo) {
        long id = databaseHelper.update(userInfo);
        if (id != -1) {
            if (userInfo.faceFeature != null) {
                FaceManager.getInstance().dbDel(userInfo.userId);
                FaceManager.getInstance().dbAdd(userInfo.userId, userInfo.faceFeature);
            }
            if (userInfo.palmFeature1 != null) {
                PalmManager.getInstance().dbDel(userInfo.userId);
                PalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature1);
            }
            if (userInfo.palmFeature2 != null) {
                NIRPalmManager.getInstance().dbDel(userInfo.userId);
                NIRPalmManager.getInstance().dbAdd(userInfo.userId, userInfo.palmFeature2);
            }
        }
        //update memory
        synchronized (BioDataUtil.userFaces_all_List) {
            //find special user
            for (UserInfo user : userFaces_all_List) {
                if (user.userId.equals(userInfo.userId)) {
                    user.name = userInfo.name;
                    user.userId = userInfo.userId;
                    user.faceFeature = userInfo.faceFeature;
                    user.palmFeature1 = userInfo.palmFeature1;
                    user.palmFeature2 = userInfo.palmFeature2;
                    user.age = userInfo.age;
                    user.gender = userInfo.gender;
                    user.avatarIndex = userInfo.avatarIndex;
                    break;
                }
            }
        }
        return id;
    }

    public void deleteAll() {
        synchronized (BioDataUtil.userFaces_all_List) {
            userFaces_all_List.clear();
            FaceManager.getInstance().dbClear();
            PalmManager.getInstance().dbClear();
            NIRPalmManager.getInstance().dbClear();
            RecognizedBioDataCache.instance().clearRecFaces();
        }
        Log.w(TAG, "deleteAll: clear list");
        databaseHelper.deleteAllUserInfo();
        Log.w(TAG, "deleteAll: clear database");
        File pic_folder = new File(Config.getHostAvatarPath());
        if (pic_folder.exists() && pic_folder.isDirectory()) {
            com.armatura.biomodule.util.FileUtils.cleanDirectory(pic_folder);
            pic_folder.deleteOnExit();
        }
        File palmEnrollImageDir = new File(com.armatura.biomodule.util.FileUtils.USER_BIO_PHOTO);
        if (palmEnrollImageDir.exists()) {
            FileUtils.cleanDirectory(palmEnrollImageDir);
            palmEnrollImageDir.deleteOnExit();
        }
        Log.w("FaceDataUtil", "deleteAll: clear user picture");
    }

    public boolean deleteBioDataByUserPin(String userPin) {
        if (databaseHelper.deleteUserInfoByUserPin(userPin)) {
            FaceManager.getInstance().dbDel(userPin);
            PalmManager.getInstance().dbDel(userPin);
            NIRPalmManager.getInstance().dbDel(userPin);
            String imgPath = Config.getHostAvatarPath() + userPin + ".jpg";
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                imgFile.delete();
            }
            //delete enroll image
            String enrollImageDirPath = com.armatura.biomodule.util.FileUtils.USER_BIO_PHOTO
                    + File.separator + userPin;
            File enrollImageDir = new File(enrollImageDirPath);
            if (enrollImageDir.exists()) {
                FileUtils.cleanDirectory(enrollImageDir);
                enrollImageDir.delete();
            }
            return true;
        }
        return false;
    }

    public boolean isUserExist(String userPin) {
        return databaseHelper.isUserExist(userPin);
    }

    public static int getValidLength(byte[] id) {
        int length = 40;
        for (int i = 0; i < id.length; i++) {
            if (id[i] == 0x00) {
                length = i;
                break;
            }
        }
        return length;
    }

    public int saveCardInfo(CardInfo cardInfo) {
        return databaseHelper.insertOrUpdate(cardInfo);
    }

    public CardInfo getCardInfoByUserId(String userId) {
        try {
            List<CardInfo> cardInfos = databaseHelper.cardInfoDao.queryBuilder()
                    .where()
                    .eq("userId", userId)
                    .query();
            if (!cardInfos.isEmpty()) {
                return cardInfos.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
