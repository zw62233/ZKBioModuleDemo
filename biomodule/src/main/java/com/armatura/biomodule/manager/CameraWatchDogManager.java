package com.armatura.biomodule.manager;

import com.armatura.biomodule.common.WatchDogType;

import java.util.concurrent.TimeUnit;

public class CameraWatchDogManager {
    private static volatile CameraWatchDogManager instance;
    private WatchDog UVCWatchDog;
    private WatchDog HIDWatchDog;

    private CameraWatchDogManager() {

    }

    public static CameraWatchDogManager getInstance() {
        if (instance == null) {
            synchronized (CameraWatchDogManager.class) {
                if (instance == null) {
                    instance = new CameraWatchDogManager();
                }
            }
        }
        return instance;
    }

    public void initCameraWatchDog(@WatchDogType int type, WatchDog.OnWatchDogTimeoutListener watchDogTimeoutListener, boolean isDebug) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    UVCWatchDog.stopWatchDog();
                    UVCWatchDog = null;
                }
                UVCWatchDog = new WatchDog(watchDogTimeoutListener);
                UVCWatchDog.isDebug(isDebug);
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    HIDWatchDog.stopWatchDog();
                    HIDWatchDog = null;
                }
                HIDWatchDog = new WatchDog(watchDogTimeoutListener);
                HIDWatchDog.isDebug(isDebug);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void feedCameraWatchDog(@WatchDogType int type) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    UVCWatchDog.feedDog();
                }
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    HIDWatchDog.feedDog();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void startFeedWatchDog(@WatchDogType int type, int initFoodCount, int initialDelay, int period, TimeUnit unit) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    try {
                        UVCWatchDog.startFeedDog(initFoodCount, initialDelay, period, unit);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    try {
                        HIDWatchDog.startFeedDog(initFoodCount, initialDelay, period, unit);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void pauseFeedWatchDog(@WatchDogType int type) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    UVCWatchDog.pauseEatDogFood();
                }
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    HIDWatchDog.pauseEatDogFood();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void resumeFeedWatchDog(@WatchDogType int type) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    UVCWatchDog.resumeFeedDog();
                }
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    HIDWatchDog.resumeFeedDog();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void stopFeedWatchDog(@WatchDogType int type) {
        switch (type) {
            case WatchDogType.UVC: {
                if (UVCWatchDog != null) {
                    UVCWatchDog.stopWatchDog();
                }
                break;
            }
            case WatchDogType.HID: {
                if (HIDWatchDog != null) {
                    HIDWatchDog.stopWatchDog();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported watch type.");
        }
    }

    public void pause() {
        if (UVCWatchDog != null) {
            UVCWatchDog.pauseEatDogFood();
        }
        if (HIDWatchDog != null) {
            HIDWatchDog.pauseEatDogFood();
        }
    }

    public void resume() {
        if (UVCWatchDog != null) {
            UVCWatchDog.resumeFeedDog();
        }
        if (HIDWatchDog != null) {
            HIDWatchDog.resumeFeedDog();
        }
    }

    public void stop() {
        if (UVCWatchDog != null) {
            UVCWatchDog.stopWatchDog();
        }
        if (HIDWatchDog != null) {
            HIDWatchDog.stopWatchDog();
        }
    }
}