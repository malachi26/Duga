package net.zomis.duga

import net.zomis.duga.tasks.ListenTask
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.TaskScheduler

/**
 * Keeps track of what rooms to listen in and regularly tell those rooms to do their listening
 */
class DugaChatListener implements InitializingBean {

    @Autowired TaskScheduler scheduler
    @Autowired DugaBot chatBot
    @Autowired DugaTasks tasks

    private ChatCommands commands

    @Override
    void afterPropertiesSet() throws Exception {
        assert !commands
        commands = new ChatCommands(this)
        scheduler.scheduleWithFixedDelay(new ListenTask(chatBot, '20298', commands), 3000)
    }

}