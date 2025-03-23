package com.talhanation.workers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class MerchantResetCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("workers").requires((source) -> source.hasPermission(2));

        literalBuilder
            .then(Commands.literal("merchants")
            .then(Commands.literal("resetAllCreativeTrades").executes( (commandSource) -> {
                List<MerchantEntity> merchants =  getListOfLoadedMerchants(commandSource.getSource().getLevel());

                for(MerchantEntity merchant : merchants){
                    merchantResetTrades(merchant);
                }
                CommandSourceStack source = commandSource.getSource();
                if (!merchants.isEmpty()) {
                    source.sendSuccess(()-> Component.literal("Successfully reset trades for " + merchants.size() + " merchants."), true);
                    return 1;
                } else {
                    source.sendFailure(Component.literal("No merchants found to reset trades.").withStyle(ChatFormatting.RED));
                    return 0;
                }
            })));

        dispatcher.register(literalBuilder);
    }

    private static List<MerchantEntity> getListOfLoadedMerchants(ServerLevel level) {
        return StreamSupport.stream(level.getEntities().getAll().spliterator(), false)
                .filter(entity -> entity instanceof MerchantEntity merchant && merchant.isCreative())
                .map(MerchantEntity.class::cast)
                .collect(Collectors.toList());
    }
    private static void merchantResetTrades(MerchantEntity merchant){
            for(int i = 0; i < MerchantEntity.TRADE_SLOT.length; i++)
                merchant.setCurrentTrades(i, 0);
    }
}
