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
package com.txusballesteros.bubbles.app;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.txusballesteros.bubbles.BubbleBounceInterpolator;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.BubblesService;
import com.txusballesteros.bubbles.OnInitializedCallback;

public class MainActivity extends AppCompatActivity {

    private BubblesManager bubblesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBubblesManager();

        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewBubble();
            }
        });
        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removedBubbles();
            }
        });
    }

    private void removedBubbles() {
        bubblesManager.clear();
    }

    private void addNewBubble() {
        final BubbleLayout bubbleView = (BubbleLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.bubble_layout, null);
        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
            @Override
            public void onBubbleRemoved(BubbleLayout bubble) {
            }
        });

        final AlertDialog[] dialog = new AlertDialog[1];

        final View dialogView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.view_dialog, null, false);
        (dialogView.findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bubblesManager.removeDialog(bubbleView, dialog[0]);
                dialog[0] = null;
            }
        });

        bubbleView.setDialogView(dialogView);

        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {

            @Override
            public void onBubbleClick(final BubbleLayout bubble) {
                Toast.makeText(getApplicationContext(), "Clicked !",
                        Toast.LENGTH_SHORT).show();

                if (bubble.getDialogView() != null) {
                    dialog[0] = bubblesManager.addDialogView(bubbleView, bubble.getDialogView(), null, null);
                }
            }
        });
        bubbleView.setOnHoldingBubbleListener(new BubbleLayout.OnHoldingBubbleListener() {
            @Override
            public void onHoldingBubble(BubbleLayout bubble) {
                Toast.makeText(getApplicationContext(), "You are holding Bubble!",
                        Toast.LENGTH_SHORT).show();
            }
        });
        bubbleView.setOnBubbleStickToWallListener(new BubbleLayout.OnBubbleStickToWallListener() {
            @Override
            public void onBubbleStickToWall(BubbleLayout bubble, boolean leftSide) {
                String side = leftSide ? "left side" : "right side";

                Toast.makeText(getApplicationContext(), "Bubble has stick on " + side + " wall",
                        Toast.LENGTH_SHORT).show();
            }
        });
        bubbleView.setShouldStickToWall(true);
        bubbleView.setTag("Click");
        bubblesManager.addBubble(bubbleView, 60, 20);
    }

    private void initializeBubblesManager() {
        bubblesManager = new BubblesManager.Builder(this)
                .setAllowRedundancies(false)
                .setTrashLayout(R.layout.bubble_trash_layout)
                .setTrashAnimations(R.animator.bubble_trash_shown_animator, R.animator.bubble_trash_hide_animator)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        addNewBubble();
                    }
                })
                .setRedundancyAnimation(new BubblesService.RedundancyAnimationListener() {
                    @Override
                    public void redundanciesAnimation(BubbleLayout bubble) {
                        ObjectAnimator
                                .ofFloat(bubble, "translationX", 0, 25, 0, 25, -0, 15, -0, 6, -0, 0)
                                .setDuration(1000)
                                .start();
                    }
                })
                .setOnShowDialogViewAnimation(new BubblesService.OnShowingDialogViewAnimationListener() {

                    @Override
                    public void onShowingDialogViewAnimation(AlertDialog alertDialog, BubbleLayout bubbleView, View view) {
                        if (alertDialog.getWindow() != null) {
                            final View dialogView = alertDialog.getWindow().getDecorView();

                            final int centerX = dialogView.getWidth() / 2;
                            final int centerY = dialogView.getHeight() / 2;
                            float startRadius = 20;
                            float endRadius = dialogView.getHeight();

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                Animator animator = ViewAnimationUtils.createCircularReveal(dialogView, centerX, centerY, startRadius, endRadius);
                                animator.setDuration(1000);
                                animator.start();
                            }

                            final Animation animShake = AnimationUtils.loadAnimation(view.getContext(), com.txusballesteros.bubbles.R.anim.shake);
                            BubbleBounceInterpolator interpolator = new BubbleBounceInterpolator(0.2, 20);
                            animShake.setInterpolator(interpolator);
                            view.startAnimation(animShake);
                        }
                    }
                })
                .build();
        bubblesManager.initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bubblesManager.recycle();
    }
}
