package cv.igrp.sdktest.harness;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScopedInvoiceRepository extends JpaRepository<ScopedInvoice, Long> {
}
