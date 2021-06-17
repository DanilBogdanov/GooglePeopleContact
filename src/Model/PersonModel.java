package Model;

import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

public class PersonModel {
    private Person person;

    public static List<PersonModel> getPersonModelFromPerson(List<Person> personList) {
        List<PersonModel> result = new ArrayList<>();
        for (Person person : personList) {
            result.add(new PersonModel(person));
        }
        return result;
    }

    public PersonModel(Person person) {
        this.person = person;
    }

    public String getName() {
        String result = person.getNames().get(0).getDisplayName();
        if (result != null) {
            return result;
        } else {
            return person.getNames().get(0).getGivenName();
        }
    }

    public String getPhoneNumber() {
        String result = "";
        try {
            List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();
            if (phoneNumbers != null) {
                for (PhoneNumber phoneNumber : phoneNumbers) {
                    result += phoneNumber.getValue() + "  ";
                }
            } else {
                result += "---";
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't get phone number");
        }
        return result;
    }
}
