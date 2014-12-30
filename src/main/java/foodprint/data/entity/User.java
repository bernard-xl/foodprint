package foodprint.data.entity;

/**
 * Created by bernard on 21/12/14.
 */
public class User {

    private Long id;

    private String name;

    private String photoPath;

    public User() {
    }

    public User(String name, String photoPath) {
        this.name = name;
        this.photoPath = photoPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
