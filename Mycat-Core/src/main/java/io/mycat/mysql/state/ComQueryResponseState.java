package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryResponseCallback;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * COM_QUERY响应
 *
 * @author ynfeng
 */
public class ComQueryResponseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryResponseState.class);
    public static final ComQueryResponseState INSTANCE = new ComQueryResponseState();

    private BackendComQueryResponseCallback backendComQueryResponseCallback = new BackendComQueryResponseCallback();

    private ComQueryResponseState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ComQueryResponseState");
        byte packageType = mySQLFrontConnection.getCurrentPacketType();
        if (packageType != MySQLPacket.ERROR_PACKET) {
            mySQLFrontConnection.setNextState(ComQueryColumnDefState.INSTANCE);
        } else {
            ConDataBuffer writeBuffer = mySQLFrontConnection.getWriteDataBuffer();
            mySQLFrontConnection.setWriteDataBuffer(mySQLFrontConnection.getShareBuffer());
            mySQLFrontConnection.setNextState(IdleState.INSTANCE);
            mySQLFrontConnection.setWriteCompleteListener(() -> {
                mySQLFrontConnection.setCurrentPacketStartPos(0);
                mySQLFrontConnection.getWriteDataBuffer().clear();
                mySQLFrontConnection.getReadDataBuffer().clear();
                mySQLFrontConnection.setWriteDataBuffer(writeBuffer);
            });
            mySQLFrontConnection.enableWrite(true);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in ComQueryResponseState");
        try {
            backendComQueryResponseCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getReadDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
            mySQLBackendConnection.getMySQLFrontConnection().driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("Backend ComQueryResponseState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
