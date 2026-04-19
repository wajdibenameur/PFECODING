package tn.iteam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monitored_host")
public class MonitoredHost {
    @Id
    @Column(nullable = false)
    private String hostId;
    @Column(nullable = false)
    private String name;
    private String ip;
    private Integer port;
    @Column(nullable = false)
    private String source;
}
