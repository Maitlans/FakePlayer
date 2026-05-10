package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@Singleton
public class KillallCommand extends AbstractCommand {

    /**
     */
    public void killall(@NotNull CommandSender sender, @NotNull CommandArguments args) {
        manager.removeAll("Command killall");
    }

}
