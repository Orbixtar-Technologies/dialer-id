package com.example.service

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DialerConnectionService : ConnectionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = DialerConnection()
        connection.connectionCapabilities = Connection.CAPABILITY_SUPPORT_HOLD or Connection.CAPABILITY_MUTE
        connection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setInitializing()
        connection.bindToSipCallState()
        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val failed = super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
        Toast.makeText(this, "Unable to place the call", Toast.LENGTH_SHORT).show()
        return failed
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    inner class DialerConnection : Connection() {
        private var stateJob: Job? = null
        private var reportedActive = false

        fun bindToSipCallState() {
            stateJob?.cancel()
            stateJob = serviceScope.launch {
                CallManager.getInstance(this@DialerConnectionService).callState.collect { info ->
                    when (info.phase) {
                        CallPhase.INITIALIZING, CallPhase.DIALING, CallPhase.CONNECTING, CallPhase.RINGING, CallPhase.EARLY_MEDIA -> {
                            setDialing()
                        }
                        CallPhase.CONNECTED, CallPhase.ACTIVE -> {
                            if (!reportedActive) {
                                reportedActive = true
                                setActive()
                            }
                        }
                        CallPhase.ON_HOLD -> {
                            setOnHold()
                            reportedActive = false
                        }
                        CallPhase.ENDING -> {
                            // Call termination in progress
                        }
                        CallPhase.ENDED -> {
                            val cause = when {
                                info.sipResponseCode == 486 || info.endReason.contains("busy", ignoreCase = true) ->
                                    DisconnectCause(DisconnectCause.BUSY, info.endReason)
                                info.sipResponseCode == 603 || info.endReason.contains("decline", ignoreCase = true) ->
                                    DisconnectCause(DisconnectCause.REJECTED, info.endReason)
                                info.sipResponseCode == 408 || info.endReason.contains("timeout", ignoreCase = true) ->
                                    DisconnectCause(DisconnectCause.TIMED_OUT, info.endReason)
                                info.sipResponseCode == 404 || info.endReason.contains("not found", ignoreCase = true) ->
                                    DisconnectCause(DisconnectCause.ERROR, info.endReason)
                                info.durationSeconds > 0 ->
                                    DisconnectCause(DisconnectCause.LOCAL, info.endReason)
                                else ->
                                    DisconnectCause(DisconnectCause.LOCAL, info.endReason)
                            }
                            setDisconnected(cause)
                            destroy()
                            stateJob?.cancel()
                        }
                        CallPhase.IDLE -> {
                            // Stay initializing until Linphone reports progress.
                        }
                    }
                }
            }
        }

        override fun onDisconnect() {
            stateJob?.cancel()
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
            CallManager.getInstance(this@DialerConnectionService).endCall("Disconnected via System Telecom")
        }

        override fun onAbort() {
            stateJob?.cancel()
            setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
            destroy()
            CallManager.getInstance(this@DialerConnectionService).endCall("Call Aborted")
        }

        override fun onHold() {
            setOnHold()
        }

        override fun onUnhold() {
            if (CallManager.getInstance(this@DialerConnectionService).callState.value.phase == CallPhase.ACTIVE) {
                setActive()
            }
        }

        override fun onPlayDtmfTone(c: Char) {
            CallManager.getInstance(this@DialerConnectionService).sendDtmfTone(c)
        }
    }
}
