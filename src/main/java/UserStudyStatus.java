import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.validator.routines.EmailValidator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//статический утилитный класс. Единственный публичный метод получает на вход e-mail (для идентификации пользователя в базе) и новые данные по номеру и названию главы.

public class UserStudyStatus {
    private static ArrayList<User> tempUserBase = new ArrayList<>();

    public static void changeUserStudyStatus(String email, int currentChapterNumber, String currentChapterName) {
        getUsersFromBase();

        if (isEmailValid(email) && !isNoUserCoincidence(email)) {
            int index = 0;

            for (int i = 0; i < tempUserBase.size(); i++) {
                if (tempUserBase.get(i).getEmail().equals(email)) {
                    index = i;
                }
            }

            tempUserBase.get(index).setCurrentChapterNumber(currentChapterNumber);
            tempUserBase.get(index).setCurrentChapterName(currentChapterName);
            putUsersToBase();
        }
        else {
            System.out.println("Ошибка e-mail");
        }
    }

    private static boolean isEmailValid(String email) { //проверка валидности e-mail
        EmailValidator eValidator = EmailValidator.getInstance();
        return eValidator.isValid(email);
    }

    private static boolean isNoUserCoincidence(String email) { //проверка отсутствия совпадения переданного e-mail с базой пользователей. Возвращает true при отсутствии совпадений
        long coincidence = tempUserBase.stream()
                .filter(user -> user.getEmail().equals(email))
                .count();
        System.out.println(coincidence);
        return coincidence == 0;
    }

    private static void getUsersFromBase() { //получение пользователей из файла-базы во временное хранилище
        File file = new File("users.json");

        try (BufferedReader toRead = new BufferedReader(new FileReader(file))) {
            tempUserBase.addAll(Arrays.asList(new Gson().fromJson(toRead.lines().collect(Collectors.joining()), User[].class)));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void putUsersToBase() { //перезапись базы с измененным пользователем
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
}