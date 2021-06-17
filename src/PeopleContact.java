import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeopleContact {
    private static final String APPLICATION_NAME = "Google People API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    //appear after check-in in browser
    private static final String TOKENS_DIRECTORY_PATH = "tokens";


    private static final List<String> SCOPES = Arrays.asList(PeopleServiceScopes.CONTACTS);
    //service account of google
    private static final String CREDENTIALS_FILE_PATH = "/resources/credentials.json";
    private static PeopleService service;
    private static String resourseNameOfGroupInternet = "contactGroups/1c4391158e935c8b";

    static {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = PeopleContact.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static ListConnectionsResponse getResponse(String nextPageToken) throws IOException {
        ListConnectionsResponse response = service.people().connections()
                .list("people/me")
                .setPageSize(2000)// max 2000
                .setPageToken(nextPageToken)
                .setPersonFields("names,phoneNumbers")
                .execute();
        return response;
    }

    public static List<Person> getPersons() throws IOException {
        String nextPageToken = null;
        ListConnectionsResponse response = getResponse(nextPageToken);
        List<Person> personsList = response.getConnections();
        nextPageToken = response.getNextPageToken();

        while (nextPageToken != null) {
            response = getResponse(nextPageToken);
            personsList.addAll(response.getConnections());
            nextPageToken = response.getNextPageToken();
            if (nextPageToken == null) {
                break;
            }
        }

        return personsList;
    }

    public static Person addPerson(Person personToCreate) throws IOException {
        List<Membership> membership = new ArrayList<>();
        membership.add(new Membership().setContactGroupMembership(new ContactGroupMembership().setContactGroupResourceName(resourseNameOfGroupInternet)));
        personToCreate.setMemberships(membership);

        Person createdContact = service.people()
                .createContact(personToCreate)
                .execute();
        return createdContact;
    }

    public static Person addPerson(String name, String phoneNumber) throws IOException {
        Person contactToCreate = new Person();
        List names = new ArrayList<>();
        names.add(new Name().setGivenName(name));
        List phones = new ArrayList<>();
        phones.add(new PhoneNumber().setValue(phoneNumber));
        List<Membership> membership = new ArrayList<>();
        membership.add(new Membership().setContactGroupMembership(new ContactGroupMembership().setContactGroupResourceName(resourseNameOfGroupInternet)));
        contactToCreate.setMemberships(membership);
        contactToCreate.setNames(names);
        contactToCreate.setPhoneNumbers(phones);

        Person createdContact = service.people()
                .createContact(contactToCreate)
                .execute();
        return createdContact;
    }

    public static void updatePerson(String personId, String newPersonName, List<PhoneNumber> newPersonPhone) throws IOException {
        String personField = "";

        Person personToUpdate = service.people()
                .get(personId)
                .setPersonFields("names,phoneNumbers")
                .execute();
        if (newPersonName != null) {
            personField += "names,";
            List<Name> names = new ArrayList<>();
            names.add(new Name().setGivenName(newPersonName));
            personToUpdate.setNames(names);
        }

        if (newPersonPhone != null) {
            personField += "phoneNumbers,";
            personToUpdate.setPhoneNumbers(newPersonPhone);
        }

        service.people().updateContact(personId, personToUpdate).setUpdatePersonFields(personField).execute();
    }

    public static void updatePerson(String personId, String newPersonName) throws IOException {
        updatePerson(personId, newPersonName, null);
    }

    public static void updatePerson(String personId, List<PhoneNumber> newPersonPhone) throws IOException {
        updatePerson(personId, null, newPersonPhone);
    }

    public static void deletePerson(String personId) throws IOException {
        service.people().deleteContact(personId).execute();
    }

    public static String parsingName(String name) {
        String resultName = name;
        resultName = resultName.replaceAll("[;,!]", "");
        resultName = resultName.replaceAll("\\s{2,}", " ");
        resultName = resultName.trim();

        return resultName;
    }

    public static Person getPersonFromString(String line) {
        String name = line;
        Pattern pattern = Pattern.compile("8-\\d{3}-\\d{2}-\\d{2}-\\d{3}");
        Matcher matcher = pattern.matcher(line);
        List<Name> names = new ArrayList<>();
        List<PhoneNumber> phoneNumbers = new ArrayList<>();

        while (matcher.find()) {
            String phoneNumberString = matcher.group();
            name = name.replaceAll(phoneNumberString, "");
            PhoneNumber phoneNumber = new PhoneNumber().setValue(phoneNumberString);
            phoneNumbers.add(phoneNumber);
        }

        name = parsingName(name);

        if (!name.isEmpty()) {
            names.add(new Name().setGivenName(name));
        } else {
            System.out.println("name is empty from line : " + line);
            return null;
        }

        if (phoneNumbers.isEmpty()) {
            System.out.println("phone didn't find from line : " + line);
            return null;
        }

        Person person = new Person();
        person.setNames(names);
        person.setPhoneNumbers(phoneNumbers);

        return person;
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

    }

    public static void printPeople(List<Person> connections) {
        if (connections != null && connections.size() > 0) {
            for (Person person : connections) {
                List<Name> names = person.getNames();
                System.out.println("resource name is : " + person.getResourceName());
                if (names != null && names.size() > 0) {
                    System.out.println("Name: " + person.getNames().get(0)
                            .getDisplayName());
                    List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();
                    if (phoneNumbers != null && person.size() > 0) {
                        System.out.println("Value : " + phoneNumbers.get(0).getValue());
                        System.out.println("Canonical form : " + phoneNumbers.get(0).getCanonicalForm());
                        System.out.println();
                    }
                } else {
                    System.out.println("No names available for connection.");
                }
            }
        } else {
            System.out.println("No connections found.");
        }
        System.out.println("size of collection : " + connections.size());
    }
}
