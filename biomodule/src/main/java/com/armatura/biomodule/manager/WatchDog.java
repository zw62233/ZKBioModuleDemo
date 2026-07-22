package com.armatura.biomodule.manager;

import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WatchDog {
    private static final String TAG = "WatchDog";
    private final static int DEFAULT_DOG_FOOD_COUNT = 10;
    private final static int MAX_RESET_TIME_INTERVAL = 10 * 1000;
    public static boolean debug = false;
    private final Object LOCK = new Object();
    private int thrId = 0;
    private EatFoodRunnable eatFoodRunnable = null;
    private int dogFoodRemain = DEFAULT_DOG_FOOD_COUNT;

    private volatile boolean isPauseDogEatFood = false;

    private volatile boolean isStartFeedDog = false;

    private final ScheduledThreadPoolExecutor exec;
    private long lastSendBroadTime = 0L;


    public WatchDog(OnWatchDogTimeoutListener onWatchDogTimeoutListener) {
        eatFoodRunnable = new EatFoodRunnable(onWatchDogTimeoutListener, this);
        exec = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                thrId++;
                return new Thread(r, "watchConsume_thr" + thrId);
            }
        });
    }

    public void isDebug(boolean isDebug) {
        debug = isDebug;
    }

    public void feedDog() {
        synchronized (LOCK) {
            if (dogFoodRemain <= DEFAULT_DOG_FOOD_COUNT) {
                dogFoodRemain++;
                if (debug) Log.i(TAG, "feed Dog,dog's food increase ,food remain " + dogFoodRemain);
            }
        }
    }

    public void pauseEatDogFood() {
        synchronized (LOCK) {
            isPauseDogEatFood = true;
            if (debug) Log.i(TAG, "dog food box is locked,dog can't eat food");
        }
    }

    public void resumeFeedDog() {
        synchronized (LOCK) {
            isPauseDogEatFood = false;
            //when we resume feed dog ,we need fill it's food box
            dogFoodRemain = DEFAULT_DOG_FOOD_COUNT;
            if (debug) Log.i(TAG, "dog food box is unlock,dog can eat food");
        }
    }

    public void startFeedDog(int initFoodCount, int initialDelay, int period, TimeUnit unit) throws Exception {
        if (exec == null) {
            throw new Exception("ScheduledThreadPoolExecutor is not init");
        }
        if (eatFoodRunnable == null) {
            throw new Exception("EatFoodRunnable is not init");
        }
        dogFoodRemain = initFoodCount;
        if (!isStartFeedDog) {
            exec.scheduleWithFixedDelay(eatFoodRunnable, initialDelay, period, unit);
            isStartFeedDog = true;
        }
        isPauseDogEatFood = false;
        if (debug)
            Log.w(TAG, "start feed fog,each " + period + " " + unit.name() + " feed one food");
    }

    public void stopWatchDog() {
        synchronized (LOCK) {
            isPauseDogEatFood = true;
            isStartFeedDog = false;
            if (exec != null) {
                exec.remove(eatFoodRunnable);
                exec.shutdown();
            }
            dogFoodRemain = DEFAULT_DOG_FOOD_COUNT;
            if (debug) Log.i(TAG, "stop watch dog success,dogFoodRemain" + dogFoodRemain);
        }
    }

    public interface OnWatchDogTimeoutListener {
        void onWatchDogTimeout();
    }

    private static class EatFoodRunnable implements Runnable {
        private final OnWatchDogTimeoutListener timeoutListener;
        private final WatchDog watchDog;

        public EatFoodRunnable(OnWatchDogTimeoutListener onWatchDogTimeoutListener, WatchDog watchDog) {
            timeoutListener = onWatchDogTimeoutListener;
            this.watchDog = watchDog;
        }

        @Override
        public void run() {
            if (watchDog.isPauseDogEatFood) {
                if (debug) Log.w(TAG, "dog food box is locked ,can't eat food ");
                return;
            }
            if (watchDog.dogFoodRemain <= 0) {
                if (SystemClock.elapsedRealtime() - watchDog.lastSendBroadTime > MAX_RESET_TIME_INTERVAL) {
                    watchDog.lastSendBroadTime = SystemClock.elapsedRealtime();
                    if (timeoutListener != null) {
                        timeoutListener.onWatchDogTimeout();
                        if (debug)
                            Log.i(TAG, "dog food remain " + watchDog.dogFoodRemain + " ,dog will died");
                    }
                } else {
                    Log.w(TAG, "two reset interval too short");
                }
            } else {
                watchDog.dogFoodRemain = watchDog.dogFoodRemain - 1;
            }
            if (debug) Log.d(TAG, "dog eat a food,dog food remain " + watchDog.dogFoodRemain);
        }
    }

}
