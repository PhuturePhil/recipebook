package com.recipebook.config;

import com.recipebook.model.Recipe;
import com.recipebook.model.Ingredient;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataLoader {

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Bean
    public CommandLineRunner loadData(RecipeRepository recipeRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setVorname("Philipp");
                admin.setNachname("Pastoors");
                admin.setEmail("philipp.pastoors@mail.de");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                
                System.out.println("Admin-Benutzer erstellt: philipp.pastoors@mail.de");
            }
            
            if (recipeRepository.count() == 0) {
                User admin = userRepository.findByEmail("philipp.pastoors@mail.de").orElse(null);
                
                Recipe carbonara = new Recipe();
                carbonara.setTitle("Spaghetti Carbonara");
                carbonara.setDescription("Klassisches italienisches Pastagericht mit Eiern, Käse und Speck");
                carbonara.setBaseServings(4);
                carbonara.setUser(admin);
                carbonara.setInstructions(Arrays.asList(
                    "Nudeln nach Packungsanleitung al dente kochen",
                    "Speck in einer Pfanne knusprig braten",
                    "Eier mit geriebenem Parmesan verquirlen",
                    "Nudeln abgießen und sofort mit der Eiermischung vermengen",
                    "Den knusprigen Speck dazugeben und servieren"
                ));
                List<Ingredient> carbonaraIngredients = Arrays.asList(
                    new Ingredient("Spaghetti", "400", "g"),
                    new Ingredient("Speck", "200", "g"),
                    new Ingredient("Eier", "4", "Stück"),
                    new Ingredient("Parmesan", "100", "g"),
                    new Ingredient("Salz", "", "nach Geschmack"),
                    new Ingredient("Pfeffer", "", "nach Geschmack")
                );
                carbonaraIngredients.forEach(ing -> ing.setRecipe(carbonara));
                carbonara.setIngredients(carbonaraIngredients);
                
                Recipe pancakes = new Recipe();
                pancakes.setTitle("Fluffige Pfannkuchen");
                pancakes.setDescription("Lockere und leckere Pfannkuchen für das Frühstück");
                pancakes.setBaseServings(4);
                pancakes.setUser(admin);
                pancakes.setInstructions(Arrays.asList(
                    "Mehl, Zucker, Backpulver und eine Prise Salz vermischen",
                    "Milch, Eier und flüssige Butter hinzufügen",
                    "Alles zu einem glatten Teig verrühren",
                    "Etwas Öl in einer Pfanne erhitzen",
                    "Teig portionsweise backen, bis beide Seiten goldbraun sind"
                ));
                List<Ingredient> pancakesIngredients = Arrays.asList(
                    new Ingredient("Mehl", "200", "g"),
                    new Ingredient("Milch", "300", "ml"),
                    new Ingredient("Eier", "2", "Stück"),
                    new Ingredient("Zucker", "2", "EL"),
                    new Ingredient("Backpulver", "1", "TL"),
                    new Ingredient("Butter", "50", "g"),
                    new Ingredient("Öl", "2", "EL")
                );
                pancakesIngredients.forEach(ing -> ing.setRecipe(pancakes));
                pancakes.setIngredients(pancakesIngredients);
                
                Recipe guacamole = new Recipe();
                guacamole.setTitle("Guacamole");
                guacamole.setDescription("Cremige Avocado-Dip mit frischem Gemüse");
                guacamole.setBaseServings(4);
                guacamole.setUser(admin);
                guacamole.setInstructions(Arrays.asList(
                    "Avocados halbieren, Kern entfernen und Fruchtfleisch herauslöffeln",
                    "Zwiebel und Knoblauch fein hacken",
                    "Tomaten würfeln und Koriander hacken",
                    "Alles mit Limettensaft, Salz und Pfeffer mischen",
                    "Kurz durchziehen lassen und servieren"
                ));
                List<Ingredient> guacamoleIngredients = Arrays.asList(
                    new Ingredient("Avocados", "3", "Stück"),
                    new Ingredient("Tomaten", "2", "Stück"),
                    new Ingredient("Zwiebel", "1", "kleine"),
                    new Ingredient("Knoblauchzehen", "2", "Stück"),
                    new Ingredient("Koriander", "1", "Bund"),
                    new Ingredient("Limettensaft", "2", "EL"),
                    new Ingredient("Salz", "1", "TL"),
                    new Ingredient("Pfeffer", "", "nach Geschmack")
                );
                guacamoleIngredients.forEach(ing -> ing.setRecipe(guacamole));
                guacamole.setIngredients(guacamoleIngredients);
                
                recipeRepository.save(carbonara);
                recipeRepository.save(pancakes);
                recipeRepository.save(guacamole);
                
                System.out.println("Testdaten geladen: 3 Rezepte erstellt");
            }
        };
    }
}
