/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.workshop.proxy.frontend.mysql.command;

import io.netty.channel.ChannelHandlerContext;
import org.apache.shardingsphere.workshop.proxy.frontend.core.api.CommandExecutor;
import org.apache.shardingsphere.workshop.proxy.frontend.core.api.QueryCommandExecutor;
import org.apache.shardingsphere.workshop.proxy.frontend.core.engine.CommandExecuteEngine;
import org.apache.shardingsphere.workshop.proxy.frontend.mysql.MySQLErrPacketFactory;
import org.apache.shardingsphere.workshop.proxy.transport.core.packet.CommandPacket;
import org.apache.shardingsphere.workshop.proxy.transport.core.packet.CommandPacketType;
import org.apache.shardingsphere.workshop.proxy.transport.core.packet.DatabasePacket;
import org.apache.shardingsphere.workshop.proxy.transport.core.payload.PacketPayload;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.packet.command.MySQLCommandPacket;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.packet.command.MySQLCommandPacketFactory;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.packet.command.MySQLCommandPacketType;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.packet.command.MySQLCommandPacketTypeLoader;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.packet.generic.MySQLEofPacket;
import org.apache.shardingsphere.workshop.proxy.transport.mysql.payload.MySQLPacketPayload;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Command execute engine for MySQL.
 */
public final class MySQLCommandExecuteEngine implements CommandExecuteEngine {
    
    @Override
    public MySQLCommandPacketType getCommandPacketType(final PacketPayload payload) {
        return MySQLCommandPacketTypeLoader.getCommandPacketType((MySQLPacketPayload) payload);
    }
    
    @Override
    public MySQLCommandPacket getCommandPacket(final PacketPayload payload, final CommandPacketType type) throws SQLException {
        return MySQLCommandPacketFactory.newInstance((MySQLCommandPacketType) type, (MySQLPacketPayload) payload);
    }
    
    @Override
    public CommandExecutor getCommandExecutor(final CommandPacketType type, final CommandPacket packet) {
        return MySQLCommandExecutorFactory.newInstance((MySQLCommandPacketType) type, packet);
    }
    
    @Override
    public DatabasePacket getErrorPacket(final Exception cause) {
        return MySQLErrPacketFactory.newInstance(1, cause);
    }
    
    @Override
    public Optional<DatabasePacket> getOtherPacket() {
        return Optional.empty();
    }
    
    @Override
    public void writeQueryData(final ChannelHandlerContext context, final QueryCommandExecutor queryCommandExecutor, final int headerPackagesCount) throws SQLException {
        if (!queryCommandExecutor.isQuery() || !context.channel().isActive()) {
            return;
        }
        int currentSequenceId = 0;
        while (queryCommandExecutor.next()) {
//            while (!context.channel().isWritable() && context.channel().isActive()) {
//                backendConnection.getResourceSynchronizer().doAwait();
//            }
            context.writeAndFlush(queryCommandExecutor.getQueryData());
            currentSequenceId++;
        }
        context.writeAndFlush(new MySQLEofPacket(++currentSequenceId + headerPackagesCount));
    }
}