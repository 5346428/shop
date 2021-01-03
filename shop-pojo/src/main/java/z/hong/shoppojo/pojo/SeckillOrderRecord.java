package z.hong.shoppojo.pojo;

import org.springframework.stereotype.Component;

import java.io.Serializable;


public class SeckillOrderRecord implements Serializable {

    private Long id;
    private String userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SeckillOrderRecord(Long id, String userId) {
        this.id = id;
        this.userId = userId;
    }
    public SeckillOrderRecord() {

    }
    @Override
    public String toString() {
        return "SeckillOrderRecord{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                '}';
    }
}
