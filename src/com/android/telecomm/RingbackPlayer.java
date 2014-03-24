/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.telecomm;

import android.telecomm.CallState;

import com.google.common.base.Preconditions;

/**
 * Plays ringback tones. Ringback is different from other tones because it operates as the current
 * audio for a call, whereas most tones play as simple timed events. This means ringback must be
 * able to turn off and on as the user switches between calls. This is why it is implemented as its
 * own class.
 */
class RingbackPlayer extends CallsManagerListenerBase {

    private final CallsManager mCallsManager;

    private final InCallTonePlayer.Factory mPlayerFactory;

    /**
     * The ID of the current call for which the ringback tone is being played.
     */
    private String mCallId;

    /**
     * The currently active player.
     */
    private InCallTonePlayer mTonePlayer;

    RingbackPlayer(CallsManager callsManager, InCallTonePlayer.Factory playerFactory) {
        mCallsManager = callsManager;
        mPlayerFactory = playerFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void onCallStateChanged(Call call, CallState oldState, CallState newState) {
        // Only operate on the foreground call.
        if (mCallsManager.getForegroundCall() == call) {

            // Treat as ending or begining dialing based on the state transition.
            if (newState == CallState.DIALING) {
                startRingbackForCall(call);
            } else if (oldState == CallState.DIALING) {
                stopRingbackForCall(call);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onForegroundCallChanged(Call oldForegroundCall, Call newForegroundCall) {
        if (oldForegroundCall != null) {
            stopRingbackForCall(oldForegroundCall);
        }

        if (newForegroundCall != null && newForegroundCall.getState() == CallState.DIALING) {
            startRingbackForCall(newForegroundCall);
        }
    }

    /**
     * Starts ringback for the specified dialing call as needed.
     *
     * @param call The call for which to ringback.
     */
    private void startRingbackForCall(Call call) {
        Preconditions.checkState(call.getState() == CallState.DIALING);
        ThreadUtil.checkOnMainThread();

        if (mCallId != null) {
            // We only get here for the foreground call so, there's no reason why there should
            // exist a current dialing call ID.
            Log.wtf(this, "Ringback player thinks there are two foreground-dialing calls.");
        }

        mCallId = call.getId();
        if (mTonePlayer == null) {
            Log.d(this, "Playing the ringback tone.");
            mTonePlayer = mPlayerFactory.createPlayer(InCallTonePlayer.TONE_RING_BACK);
            mTonePlayer.startTone();
        }
    }

    /**
     * Stops the ringback for the specified dialing call as needed.
     *
     * @param call The call for which to stop ringback.
     */
    private void stopRingbackForCall(Call call) {
        ThreadUtil.checkOnMainThread();

        if (mCallId != null && mCallId.equals(call.getId())) {
            // The foreground call is no longer dialing or is no longer the foreground call. In
            // either case, stop the ringback tone.
            mCallId = null;

            if (mTonePlayer == null) {
                Log.w(this, "No player found to stop.");
            } else {
                Log.i(this, "Stopping the ringback tone.");
                mTonePlayer.stopTone();
                mTonePlayer = null;
            }
        }
    }
}
