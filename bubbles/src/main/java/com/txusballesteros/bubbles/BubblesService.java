/*
 * Copyright Txus Ballesteros 2015 (@txusballesteros)
 *
 * This file is part of some open source application.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contact: Txus Ballesteros <txus.ballesteros@gmail.com>
 */
package com.txusballesteros.bubbles;


import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class BubblesService extends Service {
    private BubblesServiceBinder binder = new BubblesServiceBinder();
    private List<BubbleLayout> bubbles = new ArrayList<>();
    private BubbleTrashLayout bubblesTrash;
    private WindowManager windowManager;
    private BubblesLayoutCoordinator layoutCoordinator;
    private boolean allowRedundancies = true;
    private RedundancyAnimationListener redundancyAnimationListener;
    private OnShowingDialogViewAnimationListener viewAnimationListener;

    public interface RedundancyAnimationListener {
        void redundanciesAnimation(BubbleLayout bubble);
    }

    public interface OnShowingDialogViewAnimationListener {
        void onShowingDialogViewAnimation(AlertDialog alertDialog, final BubbleLayout bubbleView, final View view);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void clearBubbles() {
        for (BubbleLayout bubble : bubbles) {
            recycleBubble(bubble);
        }
        bubbles.clear();
    }

    public void setRedundancyAnimationListener(RedundancyAnimationListener redundancieAnimationListener) {
        this.redundancyAnimationListener = redundancieAnimationListener;
    }

    public void setViewAnimationListener(OnShowingDialogViewAnimationListener viewAnimationListener) {
        this.viewAnimationListener = viewAnimationListener;
    }

    private void recycleBubble(final BubbleLayout bubble) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getWindowManager().removeView(bubble);
                for (BubbleLayout cachedBubble : bubbles) {
                    if (cachedBubble == bubble) {
                        bubble.notifyBubbleRemoved();
                        bubbles.remove(cachedBubble);
                        break;
                    }
                }
            }
        });
    }

    private void recycleDialog(final BubbleLayout bubbleView, final AlertDialog dialog) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
            }
        });
    }

    private WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        return windowManager;
    }

    public void addBubble(BubbleLayout bubble, int x, int y) {
        if (!allowRedundancies && bubble.getTag() != null) {
            for (BubbleLayout bubbleLayout : bubbles) {
                if (bubble.getTag().equals(bubbleLayout.getTag())) {
                    if (redundancyAnimationListener != null) {
                        redundancyAnimationListener.redundanciesAnimation(bubbleLayout);
                    }

                    return;
                }
            }
        }

        WindowManager.LayoutParams layoutParams = buildLayoutParamsForBubble(x, y);
        bubble.setWindowManager(getWindowManager());
        bubble.setViewParams(layoutParams);
        bubble.setLayoutCoordinator(layoutCoordinator);
        bubbles.add(bubble);
        addViewToWindow(bubble);
    }

    void addTrash(int trashLayoutResourceId) {
        if (trashLayoutResourceId != 0) {
            bubblesTrash = new BubbleTrashLayout(this);
            bubblesTrash.setWindowManager(windowManager);
            bubblesTrash.setViewParams(buildLayoutParamsForTrash());
            bubblesTrash.setVisibility(View.GONE);
            LayoutInflater.from(this).inflate(trashLayoutResourceId, bubblesTrash, true);
            addViewToWindow(bubblesTrash);
            initializeLayoutCoordinator();
        }
    }

    void addTrashAnimations(int shownAnimatorResourceId, int hideAnimatorResourceId) {
        if (shownAnimatorResourceId != 0 && hideAnimatorResourceId != 0 && bubblesTrash != null) {
            bubblesTrash.setTrashAnimatorResourceIds(shownAnimatorResourceId, hideAnimatorResourceId);
        }
    }

    public void setAllowRedundancies(boolean allowRedundancies) {
        this.allowRedundancies = allowRedundancies;
    }

    public AlertDialog addDialogView(final BubbleLayout bubbleView, final View view, final DialogInterface.OnDismissListener onDismissListener, final DialogInterface.OnCancelListener onCancelListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(BubblesService.this).setView(view);
        final AlertDialog alertDialog = builder.create();

        int typeOverlay = WindowManager.LayoutParams.TYPE_PHONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            typeOverlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setType(typeOverlay);
        }

        alertDialog.setOnDismissListener(onDismissListener);
        alertDialog.setOnCancelListener(onCancelListener);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    if (viewAnimationListener != null) {
                        viewAnimationListener.onShowingDialogViewAnimation(alertDialog, bubbleView, view);
                    }
                }
            });
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (view.getParent() != null) {
                    ((ViewGroup) view.getParent()).removeView(view);
                }

                bubbleView.setOnBubbleGoToCenterListener(new BubbleLayout.OnBubbleGoToCenterListener() {
                    @Override
                    public void onBubbleGoToCenterListener(BubbleLayout bubble, final int oldX, final int oldY) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                bubbleView.setVisibility(View.INVISIBLE);
                                alertDialog.setCancelable(true);

                                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        bubbleView.setVisibility(View.VISIBLE);
                                        bubbleView.goTo(oldX, oldY);
                                    }
                                });
                            }
                        }, 1000);

                        alertDialog.setCancelable(false);
                        alertDialog.show();
                    }
                });

                bubbleView.goToCenter();
            }
        });

        return alertDialog;
    }

    private void initializeLayoutCoordinator() {
        layoutCoordinator = new BubblesLayoutCoordinator.Builder(this)
                .setWindowManager(getWindowManager())
                .setTrashView(bubblesTrash)
                .build();
    }

    private void addViewToWindow(final BubbleBaseLayout view) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getWindowManager().addView(view, view.getViewParams());
            }
        });
    }

    private WindowManager.LayoutParams buildLayoutParamsForBubble(int x, int y) {
        int typeOverlay = WindowManager.LayoutParams.TYPE_PHONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            typeOverlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                typeOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        return params;
    }

    private WindowManager.LayoutParams buildLayoutParamsForTrash() {
        int x = 0;
        int y = 0;

        int typeOverlay = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            typeOverlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                typeOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        params.x = x;
        params.y = y;
        return params;
    }

    public void removeBubble(BubbleLayout bubble) {
        recycleBubble(bubble);
    }

    public void removeDialog(final BubbleLayout bubbleView, AlertDialog dialog) {
        recycleDialog(bubbleView, dialog);
    }

    public class BubblesServiceBinder extends Binder {
        public BubblesService getService() {
            return BubblesService.this;
        }
    }
}