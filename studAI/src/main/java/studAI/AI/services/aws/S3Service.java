package studAI.AI.services.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file, String userId) throws IOException {
        // Criamos o nome único (s3_key) para este usuário!
        String uniqueFileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        String s3Key = "users/" + userId + "/" + uniqueFileName;

        // Monta o pacote de envio
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        // Faz o upload de fato pra nuvem!
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return s3Key; // Devolvemos para ser salvo no banco
    }
}
