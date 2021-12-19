package db.teams.todos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.Attachment;
import com.microsoft.bot.schema.Serialization;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ToDoController {
    private final RestTemplate restTemplate;

    public ToDoController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }       

    private String GetToDoIdFromActivityText(Activity activity) {
        // Split text at 'Id='
        var splittedText = activity.getText().split("Id=");            

        return splittedText[splittedText.length-1];
    }

    private ToDo GetToDoById(Activity activity) {
        // Get id from activity's text
        String todoId = GetToDoIdFromActivityText(activity);

        // Send request
        String url = "https://jsonplaceholder.typicode.com/todos/" + todoId;

        // Get todo
        return this.restTemplate.getForObject(url, ToDo.class, todoId);
    }

    private Attachment createAdaptiveCardAttachment(String filePath, ToDo todo) throws URISyntaxException, IOException {        
        try {
            // Read JSON
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
            String adaptiveCardJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            // Replace placeholders with the actual values
            adaptiveCardJson = StringUtils.replace(adaptiveCardJson, "<USER_ID>", String.valueOf(todo.getUserId()));
            adaptiveCardJson = StringUtils.replace(adaptiveCardJson, "<ID>", String.valueOf(todo.getId()));
            adaptiveCardJson = StringUtils.replace(adaptiveCardJson, "<TITLE>", todo.getTitle());
            adaptiveCardJson = StringUtils.replace(adaptiveCardJson, "<COMPLETED>", String.valueOf(todo.getCompleted()));

            Attachment attachment = new Attachment();
            attachment.setContentType("application/vnd.microsoft.card.adaptive");
            attachment.setContent(Serialization.jsonToTree(adaptiveCardJson));

            return attachment;
        }
        catch(Exception e) {
            e.printStackTrace();
            return new Attachment();
        }
    
    }
    
    @PostMapping("/todos/")
    public Activity sendToDo(@RequestBody Activity activity) throws URISyntaxException, IOException {
        // Get ToDo
        ToDo todo = GetToDoById(activity);

        // Create adaptive card        
        Attachment cardAttachment = createAdaptiveCardAttachment("card.json", todo);

        // Send response
        Activity responseActivity = Activity.createMessageActivity();
        responseActivity.setAttachment(cardAttachment);

        return responseActivity;
    }  
}