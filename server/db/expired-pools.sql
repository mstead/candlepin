SET FOREIGN_KEY_CHECKS=0;

DELETE FROM cp_pool_attribute WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp_pool_attribute.pool_id);
DELETE FROM cp_pool_branding WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp_pool_branding.pool_id);
DELETE FROM cp2_pool_derprov_products WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp2_pool_derprov_products.pool_id);
DELETE FROM cp2_pool_provided_products WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp2_pool_provided_products.pool_id);
DELETE FROM cp2_pool_source_sub WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp2_pool_source_sub.pool_id);
DELETE FROM cp_pool_products WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp_pool_products.pool_id);
DELETE FROM cp_pool_source_sub WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp_pool_source_sub.pool_id);

-- These are empty for our test data
--DELETE FROM cp_cert_serial cs WHERE EXISTS (SELECT serial_id FROM cp_certificate cert WHERE EXISTS (SELECT certificate_id FROM cp_pool WHERE enddate < now()));
--DELETE FROM cp_certificate cert WHERE id IN (SELECT certificate_id FROM cp_pool WHERE enddate < now() AND certificate_id IS NOT NULL);
--DELETE FROM cp_entitlement ent WHERE id = (SELECT sourceentitlement_id FROM cp_pool WHERE enddate < now() AND sourceentitlement_id IS NOT NULL);


DELETE FROM cp_entitlement WHERE pool_id = (SELECT id FROM cp_pool WHERE enddate < now() AND id = cp_entitlement.pool_id);
DELETE FROM cp_pool WHERE enddate < now();

SET FOREIGN_KEY_CHECKS=1;