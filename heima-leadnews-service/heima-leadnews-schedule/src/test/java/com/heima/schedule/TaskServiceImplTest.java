package com.heima.schedule;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void addTask(){

        for(int i=0;i<5;i++){
            Task task = new Task();
            task.setTaskType(100+i);
            task.setPriority(50);
            task.setParameters("task test".getBytes());
            task.setExecuteTime(new Date().getTime()+5000*i);

            long taskId = taskService.addTask(task);
        }
    }

    @Test
    public void cancelTask(){
        taskService.cancleTask(1689173598439411713L);
    }

    @Test
    public void testpoll(){
        Task task = taskService.poll(100, 50);
        System.out.println(task);
    }
}
