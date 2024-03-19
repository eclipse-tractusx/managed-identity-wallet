package org.eclipse.tractusx.managedidentitywallets.command;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class GetCredentialsCommand {
    private String credentialId;
    private String identifier;
    private List<String> type;
    private String sortColumn;
    private String sortType;
    private int pageNumber;
    private int size;
    private boolean asJwt;
    private String callerBPN;
}
