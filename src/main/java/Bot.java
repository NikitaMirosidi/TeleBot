import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.validator.routines.EmailValidator;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Bot extends TelegramLongPollingBot {
    private HashMap<Long, String> requestBase = new HashMap<>();
    private HashMap<Long, User> tempUser = new HashMap<>();
    private ArrayList<User> tempUserBase = new ArrayList<>();

    @Override
    public String getBotUsername() {
        return TelegramConstants.BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return TelegramConstants.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().getText().equals("/start")) {
            startMessage(update);
        }

        else if (update.hasMessage() && update.getMessage().hasText() && requestBase.get(update.getMessage().getChatId()).equals("Enter")) {
            enterRequest(update);
        }

        else if (update.hasMessage() && update.getMessage().hasText() && requestBase.get(update.getMessage().getChatId()).equals("Registration")) {
            emailRegistrar(update);
        }

        else if (update.hasMessage() && update.getMessage().hasText() && requestBase.get(update.getMessage().getChatId()).equals("Group")) {
            groupRegistrar(update);
        }

        else if (update.hasMessage() && update.getMessage().hasText() && requestBase.get(update.getMessage().getChatId()).equals("Name")) {
            nameRegistrar(update);
        }

        else if (update.hasMessage() && update.getMessage().hasText() && requestBase.get(update.getMessage().getChatId()).equals("Surname")) {
            surnameRegistrar(update);
        }

        else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void sendText(long chatId, String messageText, int isKeyboard) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(Long.toString(chatId));
        sendMessageRequest.setText(messageText);

        switch (isKeyboard) {
            case 0:
                break;
            case 1:
                sendMessageRequest.setReplyMarkup(createRegistrationKeyboard());
                break;
            case 2:
                sendMessageRequest.setReplyMarkup(createStartKeyboard());
        }

        try {
            sendApiMethod(sendMessageRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startMessage(Update update) { //ответ на первичный старт
        long chatId = update.getMessage().getChatId();
        String messageText = "Привет! \n" +
                "Я Telegram бот, и я помогу подготовиться к собеседованию по JavaScrip.\n" +
                "Если ты здесь в первый раз, то тебе нужно зарегистрироваться.\n" +
                "А если ты уже тёртый калач, то давай продолжим. Жми Вход";

        sendText(chatId, messageText,1);
    }

    private ReplyKeyboard createRegistrationKeyboard() { //клавиатура для регистрации
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        keyboard.setKeyboard(
                Collections.singletonList(
                        Arrays.asList(
                                InlineKeyboardButton.builder().text("Регистрация").callbackData("Registration").build(),
                                InlineKeyboardButton.builder().text("Вход").callbackData("Enter").build()
                        )
                )
        );

        return keyboard;
    }

    private ReplyKeyboard createStartKeyboard() { //клавиатура для подтверждения готовности старта обучения
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        keyboard.setKeyboard(
                Collections.singletonList(
                        Collections.singletonList(
                                InlineKeyboardButton.builder().text("Старт").callbackData("Start").build()
                        )
                )
        );

        return keyboard;
    }

    private void handleCallbackQuery(Update update) { //реакции на колбеки по кнопкам
        String callbackQuery = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getFrom().getId();

        switch (callbackQuery) {
            case "Registration":
                requestBase.put(chatId, "Registration");
                sendText(chatId, "Начнем регистрацию. Укажи адрес электронной почты", 0);
                break;
            case "Enter":
                requestBase.put(chatId, "Enter");
                sendText(chatId, "Введите адрес электронной почты", 0);
                break;
            case "Start":
                sendText(chatId, "Гоу учиться! Я создал!", 0);
                //сюда можно дописать логику старта обучения
                break;
        }
    }

    private void enterRequest(Update update) { //проверка e-mail перед входом
        long chatId = update.getMessage().getChatId();
        String email = update.getMessage().getText().trim();

        if (validateEmail(email)) {
            if (isNoUserCoincidence(email)) {
                sendText(chatId, "Пользователь с таким e-mail не зарегистрирован. Попробуйте ще раз или пройдите регистрацию", 1);
            }
            else {
                sendText(chatId, "Вход выполнен. Если готов учиться жми Старт", 2);
            }
        }
        else {
            sendText(chatId, "Адрес электронной почты указан неверно. Попробуйте еще раз", 1);
        }
    }

    private void emailRegistrar(Update update) { //регистрация e-mail
        long chatId = update.getMessage().getChatId();
        String email = update.getMessage().getText().trim();

        if (validateEmail(email)) {
            if (!isNoUserCoincidence(email)) {
                sendText(chatId, "Пользователь с таким e-mail уже зарегистрирован. Попробуйте выполнить Вход", 1);
            }
            else {
                User user = new User();
                user.setEmail(email);
                user.setChatId(chatId);
                tempUser.put(chatId, user);
                requestBase.put(chatId, "Group");
                sendText(chatId, "Укажите название своей группы", 0);
            }
        }
        else {
            sendText(chatId, "Адрес электронной почты указан неверно. Попробуйте еще раз", 1);
        }
    }

    private void groupRegistrar(Update update) { //регистрация имени группы
        long chatId = update.getMessage().getChatId();
        String group = update.getMessage().getText().trim();
        User user = tempUser.get(chatId);
        user.setGroupName(group);
        tempUser.put(chatId, user);
        requestBase.put(chatId, "Name");
        sendText(chatId, "Укажите своё имя", 0);
    }

    private void nameRegistrar(Update update) { //регистрация имени пользователя
        long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getText().trim();
        User user = tempUser.get(chatId);
        user.setUserName(name);
        tempUser.put(chatId, user);
        requestBase.put(chatId, "Surname");
        sendText(chatId, "Укажите свою фамилию", 0);
    }

    private void surnameRegistrar(Update update) { //регистрация фамилии пользователя
        long chatId = update.getMessage().getChatId();
        String surName = update.getMessage().getText().trim();
        User user = tempUser.get(chatId);
        user.setUserSurname(surName);
        tempUser.put(chatId, user);
        putUsersToBase(chatId, tempUser);
        requestBase.put(chatId, "Start");
        sendText(chatId, "Регистрация завершена. Если готов учиться жми Старт", 2);
    }

    private void getUsersFromBase() { //получение пользователей из файла-базы во временное хранилище
        File file = new File("users.json");

        if (!file.exists()) { //если файла-базы еще нет создаем его и пару пустых пользователей в нем для последующего корректного чтения этого файла
            tempUserBase.add(new User());
            tempUserBase.add(new User());
        }
        else {
            try (BufferedReader toRead = new BufferedReader(new FileReader(file))) {
                tempUserBase.addAll(Arrays.asList(new Gson().fromJson(toRead.lines().collect(Collectors.joining()), User[].class)));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void putUsersToBase(long chatId, HashMap<Long, User> tempUser) { //перезапись файла-хранилища с новым пользователем
        tempUserBase.add(tempUser.get(chatId));
        Gson json = new GsonBuilder().setPrettyPrinting().create();
        File file = new File("users.json");

        try (BufferedWriter toWrite = new BufferedWriter(new FileWriter(file))) {
            List<String> jsonArr = tempUserBase.stream()
                    .map(json::toJson)
                    .collect(Collectors.toList());
            toWrite.append(jsonArr.toString());
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean validateEmail(String email) { //проверка валидности e-mail
        EmailValidator eValidator = EmailValidator.getInstance();
        return eValidator.isValid(email);
    }

    private boolean isNoUserCoincidence (String email) { //проверка отсутствия совпадения переданного e-mail с базой пользователей. Возвращает true при отсутствии совпадений
        getUsersFromBase();
        long coincidence = tempUserBase.stream()
                .filter(user -> user.getEmail().equals(email))
                .count();
        System.out.println(coincidence);
        return coincidence == 0;
    }
}