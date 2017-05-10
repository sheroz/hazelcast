/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.networking;

import com.hazelcast.nio.OutboundFrame;
import com.hazelcast.nio.ascii.TextChannelInboundHandler;
import com.hazelcast.nio.tcp.TcpIpConnection;

/**
 * Each {@link TcpIpConnection} has a {@link ChannelWriter} and it writes {@link OutboundFrame} instances to the socket. Copying
 * the Frame instances to the byte-buffer is done using the {@link ChannelOutboundHandler}.
 *
 * Each {@link TcpIpConnection} has its own {@link ChannelWriter} instance.
 *
 * Before Hazelcast 3.6 the name of this interface was ChannelOutboundHandler.
 *
 * @see ChannelReader
 * @see ChannelInboundHandler
 * @see EventLoopGroup
 */
public interface ChannelWriter {

    /**
     * Returns the total number of packets (urgent and non normal priority) pending to be written to the socket.
     *
     * @return total number of pending packets.
     */
    int totalFramesPending();

    /**
     * Returns the last {@link com.hazelcast.util.Clock#currentTimeMillis()} that a write to the socket completed.
     *
     * Writing to the socket doesn't mean that data has been send or received; it means that data was written to the
     * SocketChannel. It could very well be that this data is stuck somewhere in an io-buffer.
     *
     * @return the last time something was written to the socket.
     */
    long lastWriteTimeMillis();

    /**
     * Offers a Frame to be written to the socket.
     *
     * No guarantees are made that the frame is going to be written or received by the other side.
     *
     * @param frame the Frame to write.
     */
    void write(OutboundFrame frame);

    /**
     * Gets the {@link ChannelOutboundHandler} that belongs to this ChannelWriter.
     *
     * This method exists for the {@link TextChannelInboundHandler}, but probably should be deleted.
     *
     * @return the ChannelOutboundHandler
     */
    ChannelOutboundHandler getOutboundHandler();

    /**
     * Sets the protocol this ChannelWriter should use.
     *
     * This should be called only once at the beginning of the connection.
     *
     * See {@link com.hazelcast.nio.Protocols}
     *
     * @param protocol the protocol
     */
    void setProtocol(String protocol);

    /**
     * Closes this ChannelWriter.
     *
     * This method can be called from an arbitrary thread, and should only be called once. This should be coordinated
     * through the {@link TcpIpConnection#close(String, Throwable)} method.
     */
    void close();
}
