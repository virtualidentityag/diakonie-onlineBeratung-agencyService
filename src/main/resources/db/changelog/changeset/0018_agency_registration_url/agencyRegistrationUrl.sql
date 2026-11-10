ALTER TABLE `agencyservice`.`agency`
ADD COLUMN `registration_url` varchar(500) NULL DEFAULT NULL AFTER `agency_logo`;
ALTER TABLE `agencyservice`.`agency`
ADD COLUMN `registration_url_added_by` varchar(36) NULL DEFAULT NULL AFTER `registration_url`;
ALTER TABLE `agencyservice`.`agency`
ADD COLUMN `registration_url_added_date` datetime NULL DEFAULT NULL AFTER `registration_url_added_by`;
